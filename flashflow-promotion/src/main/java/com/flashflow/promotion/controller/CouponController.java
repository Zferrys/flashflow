package com.flashflow.promotion.controller;
import com.flashflow.common.context.UserContext;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.promotion.dto.CouponVO;
import com.flashflow.promotion.entity.Coupon;
import com.flashflow.promotion.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@Tag(name = "优惠券")
@RestController
@RequestMapping("/api/flashflow/promotion/coupon")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @Operation(summary = "可领取优惠券列表")
    @GetMapping("/available")
    public R<List<Coupon>> available() {
        return R.ok(couponService.getAvailableCoupons(UserContext.getUserId()));
    }

    @Operation(summary = "我的优惠券（含券详情）")
    @GetMapping("/mine")
    public R<List<CouponVO>> mine() {
        return R.ok(couponService.getUserCoupons(UserContext.getUserId()));
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/claim")
    public R<String> claim(@RequestParam Long couponId) {
        return R.ok(couponService.claimCoupon(UserContext.getUserId(), couponId));
    }

    @Operation(summary = "按商品范围筛选可用优惠券")
    @GetMapping("/by-scope")
    public R<List<Coupon>> byScope(@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long skuId) {
        return R.ok(couponService.getAvailableCouponsByScope(UserContext.getUserId(), categoryId, skuId));
    }

    @Operation(summary = "计算优惠金额")
    @GetMapping("/calculate")
    public R<BigDecimal> calculate(@RequestParam Long userCouponId, @RequestParam BigDecimal amount) {
        return R.ok(couponService.calculateDiscount(UserContext.getUserId(), userCouponId, amount));
    }

    @Operation(summary = "自动发放优惠券（注册/首单时调用）")
    @PostMapping("/auto-grant")
    public R<Void> autoGrant(@RequestParam String grantType) {
        couponService.autoGrantCoupons(UserContext.getUserId(), grantType);
        return R.ok();
    }

    @Operation(summary = "核销优惠券（下单成功时调用）")
    @PostMapping("/mark-used")
    public R<Boolean> markUsed(@RequestParam Long userCouponId, @RequestParam String orderSn) {
        return R.ok(couponService.markAsUsed(userCouponId, orderSn));
    }

    @Operation(summary = "释放优惠券（取消订单/退款时调用——内部接口）")
    @PostMapping("/internal/release")
    public R<Boolean> releaseCoupon(@RequestParam String orderSn) {
        return R.ok(couponService.releaseByOrderSn(orderSn));
    }

    // ========== 管理后台（仅管理员） ==========

    private void checkAdmin() {
        if (!UserContext.isAdmin()) throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @Operation(summary = "优惠券列表（管理后台）")
    @GetMapping("/admin/list")
    public R<List<Coupon>> adminList() { checkAdmin(); return R.ok(couponService.adminList()); }

    @Operation(summary = "新增优惠券")
    @PostMapping("/admin")
    public R<Void> adminCreate(@RequestBody Coupon coupon) { checkAdmin(); couponService.adminCreate(coupon); return R.ok(); }

    @Operation(summary = "修改优惠券")
    @PutMapping("/admin")
    public R<Void> adminUpdate(@RequestBody Coupon coupon) { checkAdmin(); couponService.adminUpdate(coupon); return R.ok(); }

    @Operation(summary = "删除优惠券")
    @DeleteMapping("/admin/{id}")
    public R<Void> adminDelete(@PathVariable Long id) { checkAdmin(); couponService.adminDelete(id); return R.ok(); }
}
