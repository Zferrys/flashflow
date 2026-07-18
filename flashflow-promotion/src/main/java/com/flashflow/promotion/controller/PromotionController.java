package com.flashflow.promotion.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.common.domain.R;
import com.flashflow.common.context.UserContext;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.promotion.entity.PromotionActivity;
import com.flashflow.promotion.entity.PromotionSku;
import com.flashflow.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 营销控制器
 */
@Tag(name = "营销引擎")
@RestController
@RequestMapping("/api/flashflow/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /** 管理员权限校验（统一入口，避免直接调用管理接口） */
    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    // ========== 活动管理 ==========

    @Operation(summary = "活动分页")
    @GetMapping("/activity/page")
    public R<IPage<PromotionActivity>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        return R.ok(promotionService.pageActivities(new Page<>(page, size), keyword));
    }

    @Operation(summary = "创建活动")
    @PostMapping("/activity")
    public R<Void> create(@Valid @RequestBody PromotionActivity activity) {
        requireAdmin();
        promotionService.createActivity(activity);
        return R.ok();
    }

    @Operation(summary = "活动详情")
    @GetMapping("/activity/{id}")
    public R<PromotionActivity> getById(@PathVariable Long id) {
        return R.ok(promotionService.getActivity(id));
    }

    @Operation(summary = "修改活动")
    @PutMapping("/activity")
    public R<Void> update(@Valid @RequestBody PromotionActivity activity) {
        requireAdmin();
        promotionService.updateActivity(activity);
        return R.ok();
    }

    @Operation(summary = "发布活动（触发 Redis 预热）")
    @PostMapping("/activity/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        requireAdmin();
        promotionService.publish(id);
        return R.ok();
    }

    @Operation(summary = "关闭活动")
    @PostMapping("/activity/{id}/close")
    public R<Void> close(@PathVariable Long id) {
        requireAdmin();
        promotionService.close(id);
        return R.ok();
    }

    @Operation(summary = "删除活动（仅草稿）")
    @DeleteMapping("/activity/{id}")
    public R<Void> deleteActivity(@PathVariable Long id) {
        requireAdmin();
        promotionService.deleteActivity(id);
        return R.ok();
    }

    // ========== 活动商品 ==========

    @Operation(summary = "添加活动商品")
    @PostMapping("/activity/{id}/sku")
    public R<Void> addSku(@PathVariable Long id, @Valid @RequestBody PromotionSku sku) {
        requireAdmin();
        sku.setActivityId(id);
        promotionService.addSku(sku);
        return R.ok();
    }

    @Operation(summary = "修改活动商品")
    @PutMapping("/activity/{id}/sku")
    public R<Void> updateSku(@PathVariable Long id, @Valid @RequestBody PromotionSku sku) {
        requireAdmin();
        sku.setActivityId(id);
        promotionService.updateSku(sku);
        return R.ok();
    }

    @Operation(summary = "删除活动商品")
    @DeleteMapping("/activity/{id}/sku/{skuId}")
    public R<Void> deleteSku(@PathVariable Long id, @PathVariable Long skuId) {
        requireAdmin();
        promotionService.deleteSku(id, skuId);
        return R.ok();
    }

    @Operation(summary = "活动商品列表")
    @GetMapping("/activity/{id}/sku")
    public R<List<PromotionSku>> getSkuList(@PathVariable Long id) {
        return R.ok(promotionService.getSkuList(id));
    }

    // ========== 秒杀 ==========

    @Operation(summary = "秒杀抢购（核心接口）")
    @PostMapping("/flash/sale")
    public R<PromotionService.FlashSaleResult> flashSale(@RequestBody PromotionService.FlashSaleRequest request) {
        return R.ok(promotionService.flashSale(request));
    }

    @Operation(summary = "活动总数统计")
    @GetMapping("/activity/count")
    public R<Long> activityCount() {
        return R.ok(promotionService.countActivities());
    }

    @Operation(summary = "释放秒杀限购（订单取消/退款时由 Order 服务调用，内部接口）")
    @PostMapping("/flash/release-lock")
    public R<Void> releaseLock(@RequestParam Long userId, @RequestParam Long skuId) {
        promotionService.releaseBuyLimit(userId, skuId);
        return R.ok();
    }

    @Operation(summary = "当前进行中的秒杀（status=1即将开始 + status=2进行中，未过期）")
    @GetMapping("/flash/now")
    public R<List<PromotionActivity>> flashNow() {
        return R.ok(promotionService.getActiveFlashSales());
    }
}
