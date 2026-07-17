package com.flashflow.promotion.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 用户优惠券视图（联表 coupon + user_coupon） */
@Data
public class CouponVO {
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer used;
    private LocalDateTime usedTime;
    private String orderSn;
    private LocalDateTime getTime;
    // 来自 coupon 表
    private String couponName;
    private Integer type;
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private String scope;
    private String scopeValue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
