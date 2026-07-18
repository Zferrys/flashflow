package com.flashflow.promotion.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.promotion.dao.PromotionActivityMapper;
import com.flashflow.promotion.dao.PromotionRecordMapper;
import com.flashflow.promotion.dao.PromotionSkuMapper;
import com.flashflow.promotion.entity.PromotionActivity;
import com.flashflow.promotion.entity.PromotionRecord;
import com.flashflow.promotion.entity.PromotionSku;
import com.flashflow.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import com.flashflow.common.util.ScriptUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 营销服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionActivityMapper activityMapper;
    private final PromotionSkuMapper promotionSkuMapper;
    private final PromotionRecordMapper promotionRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final RestTemplate restTemplate;

    @Value("${order.service.url:http://127.0.0.1:5050/api/flashflow/order}")
    private String orderServiceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ========== 活动管理 ==========

    @Override
    public IPage<PromotionActivity> pageActivities(Page<PromotionActivity> page, String keyword) {
        LambdaQueryWrapper<PromotionActivity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PromotionActivity::getName, keyword);
        }
        wrapper.orderByDesc(PromotionActivity::getCreateTime);
        return activityMapper.selectPage(page, wrapper);
    }

    @Override
    public PromotionActivity getActivity(Long id) {
        PromotionActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    @Override
    public void createActivity(PromotionActivity activity) {
        activity.setStatus(0); // 草稿
        activityMapper.insert(activity);
    }

    @Override
    public void updateActivity(PromotionActivity activity) {
        activityMapper.updateById(activity);
        // 修改活动后清缓存，防止读到旧数据
        deleteActivityCache(activity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        PromotionActivity activity = getActivity(id);
        if (activity.getStatus() != 0 && activity.getStatus() != 1 && activity.getStatus() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有草稿、待预热或进行中状态才能发布");
        }
        // 1. 先删缓存（Cache-Aside 写策略：防止并发读拿到旧数据）
        deleteActivityCache(id);
        // 2. Redis 预热（创建全部 16 个分片 key）
        warmUpRedis(id);
        // 3. 清理旧限购计数（避免 "还没买就超限购"）
        clearBuyLimitKeys(id);
        // 4. 更新 DB 状态
        refreshActivityStatus(activity);
        // 5. 二次删缓存（双删策略，防止并发读写入旧数据）
        deleteActivityCache(id);
        log.info("活动发布成功: id={}, 状态={}", id, activity.getStatus());
    }

    /** 删除活动的 Redis 缓存（Cache-Aside 写策略） */
    private void deleteActivityCache(Long activityId) {
        redissonClient.getBucket("flashflow:cache:activity:" + activityId).delete();
        try {
            redissonClient.getKeys().deleteByPattern("flashflow:cache:promotion_sku:" + activityId + ":*");
        } catch (Exception e) {
            log.warn("删除 SKU 缓存失败（非致命）: activityId={}", activityId, e);
        }
    }

    /** 根据当前时间自动更新活动状态（定时任务 + publish 时调用） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshActivityStatus(PromotionActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        int newStatus;
        if (now.isBefore(activity.getStartTime())) {
            newStatus = 1;
        } else if (now.isBefore(activity.getEndTime())) {
            newStatus = 2;
        } else {
            newStatus = 3;
        }
        if (newStatus != activity.getStatus()) {
            activity.setStatus(newStatus);
            activityMapper.updateById(activity);
            log.info("活动状态自动流转: id={}, {} → {}", activity.getId(), activity.getStatus(), newStatus);
        }
    }

    /** 定时刷新所有非终态活动的状态（每分钟执行） */
    @Override
    public int refreshExpiredActivities() {
        List<PromotionActivity> activeList = activityMapper.selectList(
                new LambdaQueryWrapper<PromotionActivity>().in(PromotionActivity::getStatus, 0, 1, 2));
        int count = 0;
        for (PromotionActivity act : activeList) {
            LocalDateTime now = LocalDateTime.now();
            int newStatus;
            if (now.isBefore(act.getStartTime())) {
                newStatus = 1;
            } else if (now.isBefore(act.getEndTime())) {
                newStatus = 2;
            } else {
                newStatus = 3;
            }
            if (!Integer.valueOf(newStatus).equals(act.getStatus())) {
                act.setStatus(newStatus);
                activityMapper.updateById(act);
                count++;
                log.info("活动状态自动流转: id={}, {}→{}", act.getId(), act.getStatus(), newStatus);
            }
        }
        return count;
    }

    @Override
    public void close(Long id) {
        PromotionActivity activity = getActivity(id);
        activity.setStatus(4);
        activityMapper.updateById(activity);
    }

    @Override
    public void deleteActivity(Long id) {
        PromotionActivity activity = getActivity(id);
        if (activity.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅草稿状态可删除");
        }
        // 级联删除关联的 SKU 和记录
        promotionSkuMapper.delete(new LambdaQueryWrapper<PromotionSku>().eq(PromotionSku::getActivityId, id));
        promotionRecordMapper.delete(new LambdaQueryWrapper<PromotionRecord>().eq(PromotionRecord::getActivityId, id));
        activityMapper.deleteById(id);
        log.info("活动已删除: id={}", id);
    }

    @Override
    public long countActivities() {
        return activityMapper.selectCount(null);
    }

    // ========== 活动商品 ==========

    @Override
    public void addSku(PromotionSku sku) {
        // 检查是否已存在
        PromotionSku exist = promotionSkuMapper.selectOne(
                new LambdaQueryWrapper<PromotionSku>()
                        .eq(PromotionSku::getActivityId, sku.getActivityId())
                        .eq(PromotionSku::getSkuId, sku.getSkuId()));
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该商品已在活动中");
        }
        promotionSkuMapper.insert(sku);
    }

    @Override
    public void updateSku(PromotionSku sku) {
        promotionSkuMapper.updateById(sku);
        // 修改 SKU 后清缓存，确保秒杀读到最新限购/库存
        deleteSkuCache(sku.getActivityId(), sku.getSkuId());
    }

    @Override
    public void deleteSku(Long activityId, Long skuId) {
        promotionSkuMapper.delete(
                new LambdaQueryWrapper<PromotionSku>()
                        .eq(PromotionSku::getActivityId, activityId)
                        .eq(PromotionSku::getSkuId, skuId));
        // 删除 SKU 后清缓存
        deleteSkuCache(activityId, skuId);
    }

    /** 删除单个 SKU 的 Redis 缓存 */
    private void deleteSkuCache(Long activityId, Long skuId) {
        redissonClient.getBucket("flashflow:cache:promotion_sku:" + activityId + ":" + skuId).delete();
    }

    @Override
    public List<PromotionSku> getSkuList(Long activityId) {
        return promotionSkuMapper.selectByActivityId(activityId);
    }

    @Override
    public List<PromotionActivity> getActiveFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        return activityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .in(PromotionActivity::getStatus, 1, 2)  // 即将开始 或 进行中
                .gt(PromotionActivity::getEndTime, now)   // 未过期
                .orderByAsc(PromotionActivity::getStatus)  // 进行中排在前面
                .orderByAsc(PromotionActivity::getStartTime));
    }

    // ========== 秒杀核心逻辑 ==========

    /** 原子扣减 + 限购检查的 Lua 脚本返回值 */
    private static final long LUA_OK = 1;
    private static final long LUA_STOCK_INSUFFICIENT = 0;
    private static final long LUA_BUY_LIMIT_EXCEEDED = -1;

    @Override
    public FlashSaleResult flashSale(FlashSaleRequest request) {
        // 0. 幂等检查（防止用户重复提交同一请求）
        String idempotentKey = "flashflow:seckill:idempotent:" + request.activityId() + ":" + request.userId();
        if (!redissonClient.getBucket(idempotentKey).setIfAbsent("1", java.time.Duration.ofSeconds(10))) {
            return new FlashSaleResult(false, "请勿重复提交", null);
        }

        // 1. 校验活动（优先从 Redis 缓存读取，减少 DB 压力）
        PromotionActivity activity = getActivityFromCache(request.activityId());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            return new FlashSaleResult(false, "活动尚未开始", null);
        }
        if (now.isAfter(activity.getEndTime())) {
            return new FlashSaleResult(false, "活动已结束", null);
        }

        // 2. 校验活动商品（优先从 Redis 缓存读取）
        PromotionSku sku = getSkuFromCache(request.activityId(), request.skuId());
        if (sku == null) {
            return new FlashSaleResult(false, "活动商品不存在", null);
        }

        // 3. 确保所有分片预热（创建全部 16 个 key，0 库存的设为 0）
        int shardCount = 16;
        ensureAllShardsWarmed(request.skuId(), sku.getStockLimit(), shardCount);

        // 4. 原子扣减 + 限购（一个 Lua 脚本完成，无竞态）
        int perUserLimit = sku.getPerUserLimit() != null ? sku.getPerUserLimit() : 0;
        int preferredShard = (int) (request.userId() % shardCount);
        int actualShard = -1;
        boolean buyLimitExceeded = false;

        for (int offset = 0; offset < shardCount; offset++) {
            int idx = (preferredShard + offset) % shardCount;
            long result = atomicDeductAndCheckLimit(
                    request.skuId(), idx,
                    request.activityId(), request.userId(),
                    request.quantity(), perUserLimit);
            if (result == LUA_OK) {
                actualShard = idx;
                break;
            }
            if (result == LUA_BUY_LIMIT_EXCEEDED) {
                buyLimitExceeded = true;
                break;  // 限购是按用户的，换分片也没用
            }
            // result == LUA_STOCK_INSUFFICIENT → 试下一个分片
        }

        if (buyLimitExceeded) {
            return new FlashSaleResult(false, "超过限购数量", null);
        }
        if (actualShard < 0) {
            return new FlashSaleResult(false, "库存不足", null);
        }

        // 库存和限购在 Lua 脚本中已原子完成，无需额外 flag
        try {
            // 5. 记录参与流水
            insertPromotionRecord(request.activityId(), request.skuId(), request.userId(), request.quantity());

            // 6. 创建订单（HTTP RPC 调用）
            String orderSn = createOrderFromFlashSale(request, sku);
            if (orderSn == null) {
                log.warn("秒杀订单创建失败，补偿释放: userId={}, skuId={}", request.userId(), request.skuId());
                compensateRelease(request.skuId(), request.quantity(), actualShard);
                compensateReleaseBuyLimit(request.activityId(), request.userId());
                compensateDeleteRecord(request.activityId(), request.skuId(), request.userId());
                return new FlashSaleResult(false, "订单创建失败", null);
            }

            // 7. 设置幂等标记，阻止 MQ 消费者端 InventoryService.deduct() 重复扣减
            String mqIdempotentKey = "flashflow:idempotent:deduct:" + orderSn + ":" + request.skuId();
            redissonClient.getBucket(mqIdempotentKey).set("1", java.time.Duration.ofSeconds(300));

            // 8. 递增 DB 已售数量（前端 SeckillDetail.vue 读取 soldCount/stockLimit 显示进度条）
            incrementSoldCount(request.skuId(), request.quantity());

            log.info("秒杀成功: userId={}, skuId={}, orderSn={}, shard={}",
                    request.userId(), request.skuId(), orderSn, actualShard);
            return new FlashSaleResult(true, "抢购成功", orderSn);
        } catch (Exception e) {
            log.error("秒杀异常，补偿释放: userId={}, skuId={}", request.userId(), request.skuId(), e);
            compensateRelease(request.skuId(), request.quantity(), actualShard);
            compensateReleaseBuyLimit(request.activityId(), request.userId());
            compensateDeleteRecord(request.activityId(), request.skuId(), request.userId());
            throw e;
        }
    }

    /**
     * 原子 Lua 脚本：扣减库存 + 检查限购，一步完成，无竞态。
     * <pre>
     * KEYS[1] = stock:{skuId}:{shard}
     * KEYS[2] = flashflow:promotion:buy_limit:{activityId}:{userId}
     * ARGV[1] = quantity
     * ARGV[2] = perUserLimit (0 = 不限购)
     * ARGV[3] = stock TTL
     * ARGV[4] = buyLimit TTL
     * </pre>
     * @return 1=成功, 0=库存不足, -1=超过限购
     */
    private long atomicDeductAndCheckLimit(Long skuId, int shard, Long activityId, Long userId,
                                           int quantity, int perUserLimit) {
        String stockKey = "stock:" + skuId + ":" + shard;
        String buyLimitKey = "flashflow:promotion:buy_limit:" + activityId + ":" + userId;
        try {
            String script =
                "local stock = redis.call('GET', KEYS[1])\n" +
                "if stock == false then\n" +
                "    return 0\n" +
                "end\n" +
                "stock = tonumber(stock)\n" +
                "local qty = tonumber(ARGV[1])\n" +
                "if stock < qty then\n" +
                "    return 0\n" +
                "end\n" +
                "local limit = tonumber(ARGV[2])\n" +
                "if limit > 0 then\n" +
                "    local bought = redis.call('GET', KEYS[2])\n" +
                "    if bought ~= false then\n" +
                "        bought = tonumber(bought)\n" +
                "        if bought >= limit then\n" +
                "            return -1\n" +
                "        end\n" +
                "    end\n" +
                "end\n" +
                "redis.call('DECRBY', KEYS[1], qty)\n" +
                "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))\n" +
                "if limit > 0 then\n" +
                "    if redis.call('EXISTS', KEYS[2]) == 1 then\n" +
                "        redis.call('INCRBY', KEYS[2], qty)\n" +
                "    else\n" +
                "        redis.call('SET', KEYS[2], qty)\n" +
                "        redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4]))\n" +
                "    end\n" +
                "end\n" +
                "return 1";
            Long result = redissonClient.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE,
                    script, RScript.ReturnType.INTEGER,
                    java.util.Arrays.asList(stockKey, buyLimitKey),
                    String.valueOf(quantity), String.valueOf(perUserLimit), "86400", "7200");
            return result != null ? result : LUA_STOCK_INSUFFICIENT;
        } catch (Exception e) {
            log.error("原子扣减+限购 Lua 异常: skuId={}, userId={}", skuId, userId, e);
            return LUA_STOCK_INSUFFICIENT; // 异常时拒绝放行
        }
    }

    /** 原子递增活动商品的已售数量（前端进度条读取 soldCount/stockLimit） */
    private void incrementSoldCount(Long skuId, int delta) {
        try {
            // 找到该 skuId 在 promotion_sku 中的 id（需要 activity 上下文，取最近一条）
            PromotionSku sku = promotionSkuMapper.selectOne(
                    new LambdaQueryWrapper<PromotionSku>()
                            .eq(PromotionSku::getSkuId, skuId)
                            .orderByDesc(PromotionSku::getId)
                            .last("LIMIT 1"));
            if (sku != null) {
                promotionSkuMapper.incrementSoldCount(sku.getId(), delta);
            }
        } catch (Exception e) {
            log.error("递增已售数量失败: skuId={}", skuId, e);
        }
    }

    /** 记录参与流水（单 SQL 原子操作，不需要事务） */
    private void insertPromotionRecord(Long activityId, Long skuId, Long userId, int quantity) {
        PromotionRecord record = new PromotionRecord();
        record.setActivityId(activityId);
        record.setSkuId(skuId);
        record.setUserId(userId);
        record.setQuantity(quantity);
        promotionRecordMapper.insert(record);
    }

    /** 补偿删除参与记录（订单创建失败时回滚，单 SQL 原子操作） */
    private void compensateDeleteRecord(Long activityId, Long skuId, Long userId) {
        try {
            promotionRecordMapper.delete(new LambdaQueryWrapper<PromotionRecord>()
                    .eq(PromotionRecord::getActivityId, activityId)
                    .eq(PromotionRecord::getSkuId, skuId)
                    .eq(PromotionRecord::getUserId, userId));
        } catch (Exception e) {
            log.error("补偿删除参与记录失败: activityId={}, skuId={}, userId={}", activityId, skuId, userId, e);
        }
    }

    /**
     * 秒杀成功后调用订单服务创建订单
     */
    private String createOrderFromFlashSale(FlashSaleRequest request, PromotionSku sku) {
        try {
            Map<String, Object> orderRequest = new java.util.HashMap<>();
            orderRequest.put("remark", "秒杀订单");
            orderRequest.put("addressSnapshot", "");
            orderRequest.put("items", List.of(Map.of(
                    "skuId", sku.getSkuId(),
                    "skuName", sku.getSkuName(),
                    "skuImage", sku.getSkuImage() != null ? sku.getSkuImage() : "",
                    "quantity", request.quantity(),
                    "price", sku.getActivityPrice()
            )));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", String.valueOf(request.userId()));
            headers.set("X-User-Role", "ROLE_USER");
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(orderRequest), headers);

            String response = restTemplate.postForObject(orderServiceUrl, entity, String.class);
            if (response != null) {
                try {
                    com.flashflow.common.domain.R<?> r = objectMapper.readValue(response,
                            objectMapper.getTypeFactory().constructParametricType(
                                    com.flashflow.common.domain.R.class, java.util.Map.class));
                    if (r.getData() instanceof java.util.Map<?, ?> dataMap) {
                        Object orderSn = dataMap.get("orderSn");
                        if (orderSn != null) {
                            return orderSn.toString();
                        }
                    }
                } catch (Exception parseEx) {
                    log.error("订单响应 JSON 解析失败: {}", response, parseEx);
                }
            }
        } catch (Exception e) {
            log.error("创建秒杀订单失败: skuId={}, userId={}", sku.getSkuId(), request.userId(), e);
        }
        return null;
    }

    // ========== 私有方法 ==========

    /**
     * 确保所有分片都已预热（创建全部 shardCount 个 key，库存为 0 的分片也设 0）。
     * 兼容后台直接改状态未走 publish() 流程的场景——首次购买时自动从 DB 恢复到 Redis。
     */
    private void ensureAllShardsWarmed(Long skuId, Integer totalStock, int shardCount) {
        if (totalStock == null || totalStock <= 0) return;
        for (int i = 0; i < shardCount; i++) {
            String key = "stock:" + skuId + ":" + i;
            if (!redissonClient.getAtomicLong(key).isExists()) {
                // 均匀分配：前 remainder 个分片多拿 1 个，避免大部分分片为 0
                int shardStock = totalStock / shardCount + (i < totalStock % shardCount ? 1 : 0);
                RAtomicLong rStock = redissonClient.getAtomicLong(key);
                rStock.set(Math.max(shardStock, 0));
                rStock.expire(24, TimeUnit.HOURS);
            }
        }
        log.info("全部分片预热完成: skuId={}, totalStock={}", skuId, totalStock);
    }

    /** 清理发布活动时残留的旧限购计数 key */
    private void clearBuyLimitKeys(Long activityId) {
        try {
            String pattern = "flashflow:promotion:buy_limit:" + activityId + ":*";
            long deleted = redissonClient.getKeys().deleteByPattern(pattern);
            if (deleted > 0) {
                log.info("清理旧限购计数: activityId={}, 删除 {} 个 key", activityId, deleted);
            }
        } catch (Exception e) {
            log.warn("清理旧限购计数失败（非致命）: activityId={}", activityId, e);
        }
    }

    /** Redis 预热：均匀分配库存到所有分片（始终创建全部 16 个 key，0 库存分片也设 0） */
    private void warmUpRedis(Long activityId) {
        List<PromotionSku> skuList = promotionSkuMapper.selectByActivityId(activityId);
        for (PromotionSku sku : skuList) {
            Integer total = sku.getStockLimit();
            if (total == null || total <= 0) continue;
            for (int i = 0; i < 16; i++) {
                String key = "stock:" + sku.getSkuId() + ":" + i;
                int shardStock = total / 16 + (i < total % 16 ? 1 : 0);
                RAtomicLong stock = redissonClient.getAtomicLong(key);
                stock.set(Math.max(shardStock, 0));
                stock.expire(24, TimeUnit.HOURS);
            }
            log.info("预热完成: skuId={}, 总库存={}, 分片分布: {}",
                    sku.getSkuId(), total, describeShardDistribution(total));
        }
    }

    /** 描述分片库存分布（日志用），如 "10个分片各1件, 6个分片0件" */
    private String describeShardDistribution(int total) {
        int per = total / 16;
        int rem = total % 16;
        if (rem == 0 && per > 0) return "16×" + per;
        if (per == 0 && rem > 0) return rem + "个分片各1件, " + (16 - rem) + "个分片0件";
        return "前" + rem + "片" + (per + 1) + "件, 余" + (16 - rem) + "片" + per + "件";
    }

    /** 补偿释放 Redis 库存（订单创建失败时回调） */
    private void compensateRelease(Long skuId, int quantity, int shardIndex) {
        try {
            String stockKey = "stock:" + skuId + ":" + shardIndex;
            redissonClient.getAtomicLong(stockKey).addAndGet(quantity);
            log.info("补偿释放库存成功: skuId={}, shard={}, quantity={}", skuId, shardIndex, quantity);
        } catch (Exception e) {
            log.error("补偿释放库存失败! skuId={}, 需要人工介入!", skuId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseBuyLimit(Long userId, Long skuId) {
        List<PromotionRecord> records = promotionRecordMapper.selectByUserAndSku(userId, skuId);
        for (PromotionRecord record : records) {
            // 1. 释放 Redis 限购计数器
            compensateReleaseBuyLimit(record.getActivityId(), userId);
            // 2. 删除参与记录（允许重新参与）
            promotionRecordMapper.deleteById(record.getId());
            // 3. 回退已售数量（订单取消/退款时库存回滚）
            incrementSoldCount(skuId, -record.getQuantity());
            log.info("秒杀限购已释放: userId={}, skuId={}, activityId={}", userId, skuId, record.getActivityId());
        }
    }

    /** 补偿释放限购计数（原子 Lua，防止计数器变为负数） */
    private void compensateReleaseBuyLimit(Long activityId, Long userId) {
        try {
            String key = "flashflow:promotion:buy_limit:" + activityId + ":" + userId;
            String script =
                    "local bought = redis.call('GET', KEYS[1])\n" +
                    "if bought ~= false and tonumber(bought) > 0 then\n" +
                    "    redis.call('DECR', KEYS[1])\n" +
                    "end\n" +
                    "return 1";
            redissonClient.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE,
                    script, RScript.ReturnType.INTEGER,
                    java.util.Collections.singletonList(key));
            log.info("补偿释放限购计数: activityId={}, userId={}", activityId, userId);
        } catch (Exception e) {
            log.error("补偿释放限购计数失败! activityId={}, userId={}", activityId, userId, e);
        }
    }

    // ========== Redis 缓存（秒杀热点数据，避免 DB 穿透）==========

    /** 从 Redis 缓存获取活动信息（miss 时回源 DB 并回填缓存，TTL=5分钟） */
    private PromotionActivity getActivityFromCache(Long activityId) {
        String cacheKey = "flashflow:cache:activity:" + activityId;
        try {
            String json = (String) redissonClient.getBucket(cacheKey).get();
            if (json != null) {
                return objectMapper.readValue(json, PromotionActivity.class);
            }
        } catch (Exception e) {
            log.warn("活动缓存读取失败，回源DB: activityId={}", activityId, e);
        }
        PromotionActivity activity = getActivity(activityId);
        try {
            redissonClient.getBucket(cacheKey).set(objectMapper.writeValueAsString(activity), 300, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("活动缓存回填失败: activityId={}", activityId, e);
        }
        return activity;
    }

    /** 从 Redis 缓存获取活动商品信息（miss 时回源 DB） */
    private PromotionSku getSkuFromCache(Long activityId, Long skuId) {
        String cacheKey = "flashflow:cache:promotion_sku:" + activityId + ":" + skuId;
        try {
            String json = (String) redissonClient.getBucket(cacheKey).get();
            if (json != null) {
                return objectMapper.readValue(json, PromotionSku.class);
            }
        } catch (Exception e) {
            log.warn("活动商品缓存读取失败，回源DB: activityId={}, skuId={}", activityId, skuId, e);
        }
        PromotionSku sku = promotionSkuMapper.selectOne(
                new LambdaQueryWrapper<PromotionSku>()
                        .eq(PromotionSku::getActivityId, activityId)
                        .eq(PromotionSku::getSkuId, skuId));
        if (sku != null) {
            try {
                redissonClient.getBucket(cacheKey).set(objectMapper.writeValueAsString(sku), 300, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("活动商品缓存回填失败: activityId={}, skuId={}", activityId, skuId, e);
            }
        }
        return sku;
    }
}
