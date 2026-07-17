package com.flashflow.inventory.service.impl;

import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.inventory.dao.InventoryLogMapper;
import com.flashflow.inventory.dao.InventoryMapper;
import com.flashflow.inventory.dao.InventoryShardMapper;
import com.flashflow.inventory.entity.Inventory;
import com.flashflow.inventory.entity.InventoryLog;
import com.flashflow.inventory.entity.InventoryShard;
import com.flashflow.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 库存服务实现（核心：分片 + Redisson 锁 + RAtomicLong 原子扣减）
 *
 * 与 promotion 模块共享 stock:{skuId}:{shard} 键，统一使用 RAtomicLong 编码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryShardMapper inventoryShardMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final RedissonClient redissonClient;

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
        // 0. 幂等检查
        if (request.orderSn() != null && !request.orderSn().isEmpty()) {
            String idempotentKey = "flashflow:idempotent:deduct:" + request.orderSn() + ":" + request.skuId();
            if (!redissonClient.getBucket(idempotentKey).setIfAbsent("1")) {
                log.info("幂等返回: 已扣减过, orderSn={}, skuId={}", request.orderSn(), request.skuId());
                return new DeductResult(true, 0, 0);
            }
            redissonClient.getBucket(idempotentKey).expire(java.time.Duration.ofSeconds(300));
        }

        // 1. 计算分片
        Inventory inventory = getBySkuId(request.skuId());
        if (inventory == null) {
            throw new BusinessException(ErrorCode.STOCK_SKU_NOT_FOUND);
        }
        int shardCount = inventory.getShardCount() != null ? inventory.getShardCount() : DEFAULT_SHARD_COUNT;
        int shardIndex = (int) (request.userId() % shardCount);

        String stockKey = "stock:" + request.skuId() + ":" + shardIndex;
        String lockKey = "lock:stock:" + request.skuId() + ":" + shardIndex;

        // 2. 懒加载
        ensureRedisWarmed(request.skuId(), stockKey);

        // 3. 获取分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, 3, java.util.concurrent.TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取库存锁失败: skuId={}, shard={}", request.skuId(), shardIndex);
                throw new BusinessException(ErrorCode.STOCK_LOCK_FAILED);
            }

            // 4. RAtomicLong 原子扣减（与 promotion 模块编码一致）
            RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);
            if (!stockCounter.isExists()) {
                log.warn("库存分片不存在: key={}", stockKey);
                return tryFallback(request, inventory, shardIndex);
            }
            long afterStock = stockCounter.addAndGet(-request.quantity());
            if (afterStock < 0) {
                stockCounter.addAndGet(request.quantity()); // 回退
                log.warn("库存不足: skuId={}, shard={}", request.skuId(), shardIndex);
                return tryFallback(request, inventory, shardIndex);
            }

            // 5. 记录分片映射（用于 release 时查找）
            redissonClient.getBucket("flashflow:inventory:deduct:shard:" + request.orderSn() + ":" + request.skuId())
                    .set(shardIndex, 300, java.util.concurrent.TimeUnit.SECONDS);

            // 6. 记录日志
            recordLog(request.skuId(), shardIndex, request.quantity(), "DEDUCT", request.orderSn());

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
        int shardIndex = findDeductedShard(skuId, orderSn);
        String stockKey = "stock:" + skuId + ":" + shardIndex;
        String lockKey = "lock:stock:" + skuId + ":" + shardIndex;

        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock(3, java.util.concurrent.TimeUnit.SECONDS);
            RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);
            stockCounter.addAndGet(quantity);
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
            RAtomicLong stockCounter = redissonClient.getAtomicLong(key);
            stockCounter.set(shard.getShardStock());
        }
        log.info("Redis 预热完成: skuId={}, 分片数={}", skuId, shards.size());
    }

    // ========== 私有方法 ==========

    /** 懒加载 Redis 分片库存 */
    private void ensureRedisWarmed(Long skuId, String targetStockKey) {
        if (redissonClient.getAtomicLong(targetStockKey).isExists()) return;
        RLock warmLock = redissonClient.getLock("lock:warmup:" + skuId);
        try {
            if (warmLock.tryLock(3, 5, java.util.concurrent.TimeUnit.SECONDS)) {
                try {
                    if (redissonClient.getAtomicLong(targetStockKey).isExists()) return;
                    warmUp(skuId);
                    log.info("懒加载 Redis 分片完成: skuId={}", skuId);
                } finally {
                    warmLock.unlock();
                }
            } else {
                log.warn("预热锁获取失败（可能其他线程正在预热）: skuId={}", skuId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("预热锁被中断: skuId={}", skuId);
        }
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
                        RAtomicLong stockCounter = redissonClient.getAtomicLong(fallbackKey);
                        if (!stockCounter.isExists()) continue;
                        long afterStock = stockCounter.addAndGet(-request.quantity());
                        if (afterStock < 0) {
                            stockCounter.addAndGet(request.quantity());
                            continue;
                        }
                        // 更新分片映射
                        redissonClient.getBucket("flashflow:inventory:deduct:shard:" + request.orderSn() + ":" + request.skuId())
                                .set(shardIndex, 300, java.util.concurrent.TimeUnit.SECONDS);
                        recordLog(request.skuId(), shardIndex, request.quantity(), "DEDUCT", request.orderSn());
                        log.info("Fallback 扣库存成功: skuId={}, shard={}", request.skuId(), shardIndex);
                        return new DeductResult(true, shardIndex, getShardStock(request.skuId(), shardIndex));
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Fallback shard {} 被中断", shardIndex);
            } catch (Exception e) {
                log.warn("Fallback shard {} 失败", shardIndex, e);
            }
        }
        throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
    }

    /** 记录库存变动日志 */
    private void recordLog(Long skuId, int shardIndex, int quantity, String type, String orderSn) {
        try {
            String stockKey = "stock:" + skuId + ":" + shardIndex;
            InventoryLog log = new InventoryLog();
            log.setSkuId(skuId);
            log.setShardIndex(shardIndex);
            log.setQuantity(quantity);
            log.setType(type);
            log.setOrderSn(orderSn);

            RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);
            int currentStock = (int) stockCounter.get();
            if ("DEDUCT".equals(type)) {
                log.setBeforeStock(currentStock + quantity);
                log.setAfterStock(currentStock);
            } else if ("RELEASE".equals(type)) {
                log.setBeforeStock(currentStock - Math.abs(quantity));
                log.setAfterStock(currentStock);
            } else {
                log.setBeforeStock(currentStock);
                log.setAfterStock(currentStock);
            }
            inventoryLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录库存日志失败: ", e);
        }
    }

    /** 获取 Redis 中某个分片的当前库存 */
    private int getShardStock(Long skuId, int shardIndex) {
        return (int) redissonClient.getAtomicLong("stock:" + skuId + ":" + shardIndex).get();
    }

    /** 查找扣减时使用的分片索引 */
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
        return 0;
    }
}
