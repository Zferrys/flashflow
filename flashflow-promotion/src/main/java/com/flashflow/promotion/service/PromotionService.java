package com.flashflow.promotion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.promotion.entity.PromotionActivity;
import com.flashflow.promotion.entity.PromotionSku;

import java.util.List;

/**
 * 营销服务接口
 */
public interface PromotionService {

    // ========== 活动管理 ==========
    IPage<PromotionActivity> pageActivities(Page<PromotionActivity> page, String keyword);
    PromotionActivity getActivity(Long id);
    void createActivity(PromotionActivity activity);
    void updateActivity(PromotionActivity activity);
    void publish(Long id);      // 发布 → 预热 Redis
    void close(Long id);

    /** 删除活动（仅草稿状态） */
    void deleteActivity(Long id);

    // ========== 活动商品 ==========
    void addSku(PromotionSku sku);
    void updateSku(PromotionSku sku);
    void deleteSku(Long activityId, Long skuId);
    List<PromotionSku> getSkuList(Long activityId);

    // ========== 统计 ==========
    long countActivities();

    /** 当前有效的秒杀活动（status=1即将开始或2进行中，且未过期） */
    List<PromotionActivity> getActiveFlashSales();

    /** 根据当前时间刷新单个活动状态 */
    void refreshActivityStatus(PromotionActivity activity);

    /** 定时刷新所有非终态活动状态 */
    int refreshExpiredActivities();

    // ========== 秒杀参与 ==========
    FlashSaleResult flashSale(FlashSaleRequest request);

    record FlashSaleRequest(Long activityId, Long skuId, Long userId, int quantity) {}
    record FlashSaleResult(boolean success, String message, String orderSn) {}
}
