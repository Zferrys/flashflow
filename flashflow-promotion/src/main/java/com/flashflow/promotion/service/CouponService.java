package com.flashflow.promotion.service;

import com.flashflow.promotion.dto.CouponVO;
import com.flashflow.promotion.entity.Coupon;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    List<Coupon> getAvailableCoupons(Long userId);
    /** 按适用范围筛选可用优惠券（用于商品详情页/购物车推荐） */
    List<Coupon> getAvailableCouponsByScope(Long userId, Long categoryId, Long skuId);
    List<CouponVO> getUserCoupons(Long userId);
    String claimCoupon(Long userId, Long couponId);
    /** 自动发放优惠券（注册/首单时触发） */
    void autoGrantCoupons(Long userId, String grantType);
    BigDecimal calculateDiscount(Long userId, Long userCouponId, BigDecimal amount);
    /** 核销：下单成功后标记优惠券已使用（乐观锁防并发） */
    boolean markAsUsed(Long userCouponId, String orderSn);

    /** 释放优惠券（根据订单号，退款/取消时调用） */
    boolean releaseByOrderSn(String orderSn);

    // ========== 管理后台 ==========

    List<Coupon> adminList();
    void adminCreate(Coupon coupon);
    void adminUpdate(Coupon coupon);
    void adminDelete(Long id);
}
