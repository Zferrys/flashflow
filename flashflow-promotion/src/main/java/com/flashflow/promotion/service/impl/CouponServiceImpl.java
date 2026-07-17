package com.flashflow.promotion.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.promotion.dao.CouponMapper;
import com.flashflow.promotion.dao.UserCouponMapper;
import com.flashflow.promotion.dto.CouponVO;
import com.flashflow.promotion.entity.Coupon;
import com.flashflow.promotion.entity.UserCoupon;
import com.flashflow.promotion.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public List<Coupon> getAvailableCoupons(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(baseAvailableQuery(now)
                .eq(Coupon::getAutoGrant, "NONE"));
    }

    @Override
    public List<Coupon> getAvailableCouponsByScope(Long userId, Long categoryId, Long skuId) {
        LambdaQueryWrapper<Coupon> wrapper = baseAvailableQuery(LocalDateTime.now());
        wrapper.and(w -> {
            w.eq(Coupon::getScope, "ALL");
            if (categoryId != null) {
                w.or(w2 -> w2.eq(Coupon::getScope, "CATEGORY")
                        .eq(Coupon::getScopeValue, String.valueOf(categoryId)));
            }
            if (skuId != null) {
                w.or(w3 -> w3.eq(Coupon::getScope, "SKU")
                        .apply("JSON_CONTAINS(scope_value, CONCAT('\"',{0},'\"'))", skuId));
            }
        });
        return couponMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void autoGrantCoupons(Long userId, String grantType) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> autoCoupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .eq(Coupon::getAutoGrant, grantType)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .gt(Coupon::getRemainCount, 0));
        if (autoCoupons.isEmpty()) return;

        // 一次查询用户已有券，避免循环 N+1
        List<UserCoupon> existing = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId));
        Map<Long, Long> perCouponCount = existing.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        for (Coupon coupon : autoCoupons) {
            long count = perCouponCount.getOrDefault(coupon.getId(), 0L);
            if (count >= coupon.getPerUserLimit()) continue;
            int affected = couponMapper.decrementRemain(coupon.getId());
            if (affected == 0) continue;
            UserCoupon uc = new UserCoupon();
            uc.setUserId(userId);
            uc.setCouponId(coupon.getId());
            userCouponMapper.insert(uc);
            log.info("自动发放优惠券: userId={}, coupon={}", userId, coupon.getName());
        }
    }

    @Override
    public List<CouponVO> getUserCoupons(Long userId) {
        return userCouponMapper.selectMyCoupons(userId);
    }

    @Override
    @Transactional
    public String claimCoupon(Long userId, Long couponId) {
        // 仅检查状态和每人限领，库存由原子 decrement 兜底
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !Objects.equals(coupon.getStatus(), 1))
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, couponId));
        if (coupon.getPerUserLimit() != null && count >= coupon.getPerUserLimit())
            throw new BusinessException(ErrorCode.BUY_LIMIT_EXCEEDED);

        int affected = couponMapper.decrementRemain(couponId);
        if (affected == 0) throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "已被抢光");

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        try {
            userCouponMapper.insert(uc);
        } catch (Exception e) {
            // 唯一索引冲突 = 重复领取，已由 uk_user_coupon(user_id,coupon_id) 兜底
            log.warn("用户重复领取: userId={}, couponId={}", userId, couponId);
            throw new BusinessException(ErrorCode.BUY_LIMIT_EXCEEDED);
        }
        log.info("用户 {} 领取优惠券 {}: {}", userId, couponId, coupon.getName());
        return coupon.getName();
    }

    @Override
    public BigDecimal calculateDiscount(Long userId, Long userCouponId, BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        UserCoupon uc = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, userCouponId)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getUsed, 0));
        if (uc == null) return BigDecimal.ZERO;
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if (coupon == null) return BigDecimal.ZERO;
        // 检查使用门槛
        if (coupon.getConditionAmount() != null
                && amount.compareTo(coupon.getConditionAmount()) < 0) return BigDecimal.ZERO;
        // type=1 满减券 → 固定优惠金额
        if (Objects.equals(coupon.getType(), 1)) return coupon.getDiscountAmount();
        // type=2 折扣券 → orderAmount * (1 - discountRate)
        if (Objects.equals(coupon.getType(), 2) && coupon.getDiscountRate() != null) {
            return amount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
        }
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public boolean markAsUsed(Long userCouponId, String orderSn) {
        // 乐观锁：UPDATE 带 WHERE used=0，防并发重复核销
        int affected = userCouponMapper.markUsed(userCouponId, orderSn, LocalDateTime.now());
        if (affected == 0) {
            log.warn("核销失败（已使用或不存在）: userCouponId={}", userCouponId);
            return false;
        }
        log.info("优惠券核销: userCouponId={}, orderSn={}", userCouponId, orderSn);
        return true;
    }

    // ========== 工具方法 ==========

    private LambdaQueryWrapper<Coupon> baseAvailableQuery(LocalDateTime now) {
        return new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .gt(Coupon::getRemainCount, 0)
                .orderByDesc(Coupon::getCreateTime);
    }

    // ========== 管理后台 CRUD（供 Controller 调用） ==========

    @Override
    public List<Coupon> adminList() {
        return couponMapper.selectList(null);
    }

    @Override
    @Transactional
    public void adminCreate(Coupon coupon) {
        coupon.setId(null);
        coupon.setCreateTime(null);
        coupon.setUpdateTime(null);
        coupon.setRemainCount(coupon.getTotalCount());
        couponMapper.insert(coupon);
    }

    @Override
    @Transactional
    public void adminUpdate(Coupon coupon) {
        coupon.setCreateTime(null);
        coupon.setUpdateTime(null);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void adminDelete(Long id) {
        couponMapper.deleteById(id);
    }
}
