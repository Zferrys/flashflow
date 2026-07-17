package com.flashflow.promotion.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        PromotionActivity activity = getActivity(id);
        // 草稿(0) 或 待预热(1) 都可以发布 → 重新根据时间计算状态 + 预热 Redis
        if (activity.getStatus() != 0 && activity.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有草稿或待预热状态才能发布");
        }
        warmUpRedis(id);
        refreshActivityStatus(activity);
        log.info("活动发布成功: id={}, 状态={}", id, activity.getStatus());
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
    }

    @Override
    public void deleteSku(Long activityId, Long skuId) {
        promotionSkuMapper.delete(
                new LambdaQueryWrapper<PromotionSku>()
                        .eq(PromotionSku::getActivityId, activityId)
                        .eq(PromotionSku::getSkuId, skuId));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlashSaleResult flashSale(FlashSaleRequest request) {
        // 1. 校验活动
        PromotionActivity activity = getActivity(request.activityId());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            return new FlashSaleResult(false, "活动尚未开始", null);
        }
        if (now.isAfter(activity.getEndTime())) {
            return new FlashSaleResult(false, "活动已结束", null);
        }

        // 2. 校验活动商品
        PromotionSku sku = promotionSkuMapper.selectOne(
                new LambdaQueryWrapper<PromotionSku>()
                        .eq(PromotionSku::getActivityId, request.activityId())
                        .eq(PromotionSku::getSkuId, request.skuId()));
        if (sku == null) {
            return new FlashSaleResult(false, "活动商品不存在", null);
        }

        // 3. 限购校验（使用 Lua 脚本原子检查）
        if (!checkBuyLimitAtomic(request.activityId(), request.userId(), sku.getPerUserLimit())) {
            return new FlashSaleResult(false, "超过限购数量", null);
        }

        // 4. 库存扣减（使用 Redisson Lua 脚本，复用 Inventory 模块的 deduct.lua）
        int shardIndex = (int) (request.userId() % 16);
        String stockKey = "stock:" + request.skuId() + ":" + shardIndex;
        String deductScript = ScriptUtil.load("lua/deduct.lua");
        RScript rScript = redissonClient.getScript();
        Long result = rScript.eval(
                RScript.Mode.READ_WRITE,
                deductScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(stockKey),
                String.valueOf(request.quantity()));
        if (result == null || result == 0) {
            return new FlashSaleResult(false, "库存不足", null);
        }

        // 5. 记录参与流水
        PromotionRecord record = new PromotionRecord();
        record.setActivityId(request.activityId());
        record.setSkuId(request.skuId());
        record.setUserId(request.userId());
        record.setQuantity(request.quantity());
        promotionRecordMapper.insert(record);

        // 6. 创建订单（调用 Order 服务）
        String orderSn = createOrderFromFlashSale(request, sku);
        if (orderSn == null) {
            // ⚠️ 补偿：订单创建失败，释放已扣减的 Redis 库存
            log.warn("秒杀订单创建失败，补偿释放库存: userId={}, skuId={}", request.userId(), request.skuId());
            compensateRelease(request.skuId(), request.quantity(), shardIndex);
            return new FlashSaleResult(false, "订单创建失败", null);
        }

        log.info("秒杀成功: userId={}, skuId={}, orderSn={}", request.userId(), request.skuId(), orderSn);
        return new FlashSaleResult(true, "抢购成功", orderSn);
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
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(orderRequest), headers);

            String response = restTemplate.postForObject(orderServiceUrl, entity, String.class);
            // 响应格式: {"code":0,"msg":"...","data":{"orderSn":"FF..."}}
            if (response != null && response.contains("\"orderSn\"")) {
                int snStart = response.indexOf("\"orderSn\":\"") + 11;
                if (snStart > 11) {
                    int snEnd = response.indexOf("\"", snStart);
                    return response.substring(snStart, snEnd);
                }
            }
            log.warn("订单服务返回异常: {}", response);
        } catch (Exception e) {
            log.error("创建秒杀订单失败: skuId={}, userId={}", sku.getSkuId(), request.userId(), e);
        }
        return null;
    }

    // ========== 私有方法 ==========

    /** Redis 预热：将活动库存加载到 Redis */
    private void warmUpRedis(Long activityId) {
        List<PromotionSku> skuList = promotionSkuMapper.selectByActivityId(activityId);
        for (PromotionSku sku : skuList) {
            // 简化预热：库存均匀分布到 16 个分片
            int perShard = sku.getStockLimit() / 16;
            for (int i = 0; i < 16; i++) {
                String key = "stock:" + sku.getSkuId() + ":" + i;
                int shardStock = (i == 15) ? perShard + sku.getStockLimit() % 16 : perShard;
                stringRedisTemplate.opsForValue().set(key, String.valueOf(shardStock), 24, TimeUnit.HOURS);
            }
            log.info("预热完成: skuId={}, 总库存={}", sku.getSkuId(), sku.getStockLimit());
        }
    }

    /**
     * 用户限购检查（Lua 脚本原子操作）
     * 替代非原子的 GET + SET 方式，防止并发竞态绕过限购
     */
    private boolean checkBuyLimitAtomic(Long activityId, Long userId, int limit) {
        String key = "flashflow:promotion:buy_limit:" + activityId + ":" + userId;
        try {
            String buyLimitScript = ScriptUtil.load("lua/buy_limit.lua");
            RScript rScript = redissonClient.getScript();
            Long result = rScript.eval(
                    RScript.Mode.READ_WRITE,
                    buyLimitScript,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(key),
                    String.valueOf(limit),
                    String.valueOf(24 * 3600) // TTL 24小时
            );
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("限购检查 Redis 异常，拒绝放行: ", e);
            return false; // Redis 异常时拒绝放行，防止攻击者故意打挂 Redis 绕过限购
        }
    }

    /** 补偿释放 Redis 库存（订单创建失败时回调） */
    private void compensateRelease(Long skuId, int quantity, int shardIndex) {
        try {
            String stockKey = "stock:" + skuId + ":" + shardIndex;
            String releaseScript = ScriptUtil.load("lua/release.lua");
            redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE, releaseScript, RScript.ReturnType.INTEGER,
                    Collections.singletonList(stockKey), String.valueOf(quantity));
            log.info("补偿释放库存成功: skuId={}, shard={}, quantity={}", skuId, shardIndex, quantity);
        } catch (Exception e) {
            log.error("补偿释放库存失败! skuId={}, 需要人工介入!", skuId, e);
        }
    }
}
