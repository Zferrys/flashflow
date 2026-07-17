package com.flashflow.inventory.service.impl;

import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.common.util.ScriptUtil;
import com.flashflow.inventory.dao.InventoryLogMapper;
import com.flashflow.inventory.dao.InventoryMapper;
import com.flashflow.inventory.dao.InventoryShardMapper;
import com.flashflow.inventory.entity.Inventory;
import com.flashflow.inventory.entity.InventoryLog;
import com.flashflow.inventory.entity.InventoryShard;
import com.flashflow.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 库存服务实现（核心：分片 + Redisson 锁 + Lua 原子扣减）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryShardMapper inventoryShardMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final RedissonClient redissonClient;

    /** Lua 脚本（通过 ScriptUtil 懒加载 + 缓存） */
    private static final String SCRIPT_DEDUCT = "lua/deduct.lua";
    private static final String SCRIPT_RELEASE = "lua/release.lua";

    /** 默认分片数 */
    private static final int DEFAULT_SHARD_COUNT = 16;

    @Override
    public Inventory getBySkuId(Long skuId) {
        return inventoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId));
    }

    @Override
    public List<InventoryShard> getShards(Long skuId) {
        return inventoryShardMapper.selectBySkuId(skuId);
    }

    @Override
    public DeductResult deduct(DeductRequest request) {
        // 1. 计算分片：user_id % shard_count
        Inventory inventory = getBySkuId(request.skuId());
        if (inventory == null) {
            throw new BusinessException(ErrorCode.STOCK_SKU_NOT_FOUND);
        }
        int shardCount = inventory.getShardCount() != null ? inventory.getShardCount() : DEFAULT_SHARD_COUNT;
        int shardIndex = (int) (request.userId() % shardCount);

        // 2. Redis 分片 key
        String stockKey = "stock:" + request.skuId() + ":" + shardIndex;
        String lockKey = "lock:stock:" + request.skuId() + ":" + shardIndex;

        // 3. 懒加载：Redis key 不存在时从 DB 回灌（首次下单自动预热，无需手动 warmUp）
        ensureRedisWarmed(request.skuId(), stockKey);

        // 4. 获取分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, 3, java.util.concurrent.TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取库存锁失败: skuId={}, shard={}", request.skuId(), shardIndex);
                throw new BusinessException(ErrorCode.STOCK_LOCK_FAILED);
            }

            // 5. 执行 Lua 脚本扣库存
            String deductScript = ScriptUtil.load(SCRIPT_DEDUCT);
            RScript rScript = redissonClient.getScript();
            Long result = rScript.eval(
                    RScript.Mode.READ_WRITE,
                    deductScript,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(stockKey),
                    String.valueOf(request.quantity())
            );

            if (result == null || result == 0) {
                log.warn("库存不足或分片耗尽: skuId={}, shard={}, 剩余={}",
                        request.skuId(), shardIndex,
                        redissonClient.getBucket(stockKey).get());
                // 尝试 fallback 到其他分片
                return tryFallback(request, inventory, shardIndex);
            }

            // 6. 记录使用的分片（用于后续释放时查找）
            redissonClient.getBucket("flashflow:inventory:deduct:shard:" + request.orderSn() + ":" + request.skuId())
                    .set(shardIndex, 300, java.util.concurrent.TimeUnit.SECONDS);

            // 7. 记录日志到 DB
            recordLog(request.skuId(), shardIndex, request.quantity(),
                     "DEDUCT", request.orderSn());

            log.info("扣库存成功: skuId={}, shard={}, quantity={}", request.skuId(), shardIndex, request.quantity());
            return new DeductResult(true, shardIndex, getShardStock(request.skuId(), shardIndex));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("扣库存异常: ", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean release(Long skuId, int quantity, String orderSn) {
        // 查找原扣减的分片（优先从 Redis 记录中恢复）
        int shardIndex = findDeductedShard(skuId, orderSn);
        String stockKey = "stock:" + skuId + ":" + shardIndex;
        String lockKey = "lock:stock:" + skuId + ":" + shardIndex;

        RLock lock = redissonClient.getLock(lockKey);
        String releaseScript = ScriptUtil.load(SCRIPT_RELEASE);

        try {
            lock.lock(3, java.util.concurrent.TimeUnit.SECONDS);
            redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    releaseScript,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(stockKey),
                    String.valueOf(quantity)
            );
            recordLog(skuId, shardIndex, -quantity, "RELEASE", orderSn);
            log.info("释放库存: skuId={}, shard={}, quantity={}", skuId, shardIndex, quantity);
            return true;
        } catch (Exception e) {
            log.error("释放库存异常: ", e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long skuId, int totalStock) {
        Inventory inventory = getBySkuId(skuId);
        boolean isNew = false;
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setSkuId(skuId);
            inventory.setShardCount(DEFAULT_SHARD_COUNT);
            isNew = true;
        }
        inventory.setTotalStock(totalStock);

        if (isNew) {
            inventoryMapper.insert(inventory);
        } else {
            inventoryMapper.updateById(inventory);
        }

        // 重置分片
        inventoryShardMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InventoryShard>()
                        .eq(InventoryShard::getSkuId, skuId));

        int perShard = totalStock / DEFAULT_SHARD_COUNT;
        for (int i = 0; i < DEFAULT_SHARD_COUNT; i++) {
            InventoryShard shard = new InventoryShard();
            shard.setSkuId(skuId);
            shard.setShardIndex(i);
            shard.setShardStock(i == DEFAULT_SHARD_COUNT - 1 ? perShard + totalStock % DEFAULT_SHARD_COUNT : perShard);
            shard.setFrozenStock(0);
            inventoryShardMapper.insert(shard);
        }
    }

    @Override
    public void warmUp(Long skuId) {
        List<InventoryShard> shards = inventoryShardMapper.selectBySkuId(skuId);
        for (InventoryShard shard : shards) {
            String key = "stock:" + skuId + ":" + shard.getShardIndex();
            redissonClient.getBucket(key).set(shard.getShardStock());
        }
        log.info("Redis 预热完成: skuId={}, 分片数={}", skuId, shards.size());
    }

    // ========== 私有方法 ==========

    /**
     * 懒加载 Redis 分片库存：快速路径检查后委托 warmUp() 完成回灌。
     */
    private void ensureRedisWarmed(Long skuId, String targetStockKey) {
        if (redissonClient.getBucket(targetStockKey).isExists()) return;
        warmUp(skuId);
        log.info("懒加载 Redis 分片完成: skuId={}", skuId);
    }

    /** Fallback：尝试其他分片 */
    private DeductResult tryFallback(DeductRequest request, Inventory inventory, int failedShard) {
        int shardCount = inventory.getShardCount() != null ? inventory.getShardCount() : DEFAULT_SHARD_COUNT;
        for (int i = 0; i < shardCount; i++) {
            if (i == failedShard) continue;
            int shardIndex = (int) ((request.userId() + i) % shardCount);
            String fallbackKey = "stock:" + request.skuId() + ":" + shardIndex;
            String fallbackLock = "lock:stock:" + request.skuId() + ":" + shardIndex;
            RLock lock = redissonClient.getLock(fallbackLock);
            try {
                if (lock.tryLock(500, 1000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    try {
                        // 直接走 Lua 原子扣减，返回值 1=成功 0=失败，避免 GET 预检 TOCTOU
                        Long result = redissonClient.getScript().eval(
                                RScript.Mode.READ_WRITE,
                                ScriptUtil.load(SCRIPT_DEDUCT),
                                RScript.ReturnType.INTEGER,
                                Collections.singletonList(fallbackKey),
                                String.valueOf(request.quantity()));
                        if (result != null && result == 1) {
                            recordLog(request.skuId(), shardIndex, request.quantity(),
                                    "DEDUCT", request.orderSn());
                            log.info("Fallback 扣库存成功: skuId={}, shard={}", request.skuId(), shardIndex);
                            return new DeductResult(true, shardIndex, getShardStock(request.skuId(), shardIndex));
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                log.warn("Fallback shard {} 失败", shardIndex, e);
            }
        }
        throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
    }

    /** 记录库存变动日志（含真实库存值，审计可追溯） */
    private void recordLog(Long skuId, int shardIndex, int quantity, String type, String orderSn) {
        try {
            String stockKey = "stock:" + skuId + ":" + shardIndex;
            InventoryLog log = new InventoryLog();
            log.setSkuId(skuId);
            log.setShardIndex(shardIndex);
            log.setQuantity(quantity);
            log.setType(type);
            log.setOrderSn(orderSn);

            // 从 Redis 读取真实库存值
            Object val = redissonClient.getBucket(stockKey).get();
            int currentStock = val instanceof Number ? ((Number) val).intValue() : 0;
            if ("DEDUCT".equals(type)) {
                log.setBeforeStock(currentStock + quantity); // 扣减前 = 当前 + 扣减量
                log.setAfterStock(currentStock);              // 扣减后 = 当前
            } else if ("RELEASE".equals(type)) {
                log.setBeforeStock(currentStock - Math.abs(quantity)); // 释放前
                log.setAfterStock(currentStock);                       // 释放后
            } else {
                log.setBeforeStock(currentStock);
                log.setAfterStock(currentStock);
            }
            inventoryLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录库存日志失败: ", e); // 日志不影响主流程
        }
    }

    /** 获取 Redis 中某个分片的当前库存 */
    private int getShardStock(Long skuId, int shardIndex) {
        Object val = redissonClient.getBucket("stock:" + skuId + ":" + shardIndex).get();
        return val instanceof Number ? ((Number) val).intValue() : -1;
    }

    /** 查找扣减时使用的分片索引（优先从 Redis 记录恢复） */
    private int findDeductedShard(Long skuId, String orderSn) {
        String mappingKey = "flashflow:inventory:deduct:shard:" + orderSn + ":" + skuId;
        Object val = redissonClient.getBucket(mappingKey).get();
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        // 未找到记录时，遍历分片找到有冻结库存的（fallback）
        List<InventoryShard> shards = inventoryShardMapper.selectBySkuId(skuId);
        for (InventoryShard shard : shards) {
            if (shard.getFrozenStock() > 0) {
                return shard.getShardIndex();
            }
        }
        // 最终默认分片0
        return 0;
    }
}
