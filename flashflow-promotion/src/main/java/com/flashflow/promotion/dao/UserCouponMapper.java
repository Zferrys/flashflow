package com.flashflow.promotion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.promotion.dto.CouponVO;
import com.flashflow.promotion.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /** 联表查询——用户优惠券+券详情 */
    @Select("SELECT uc.*, c.name AS couponName, c.type, c.condition_amount AS conditionAmount, " +
            "c.discount_amount AS discountAmount, c.discount_rate AS discountRate, " +
            "c.scope, c.scope_value AS scopeValue, c.start_time AS startTime, c.end_time AS endTime " +
            "FROM user_coupon uc INNER JOIN coupon c ON uc.coupon_id = c.id " +
            "WHERE uc.user_id = #{userId} ORDER BY uc.get_time DESC")
    List<CouponVO> selectMyCoupons(@Param("userId") Long userId);

    /** 乐观锁核销：仅当 used=0 时更新，防并发重复核销 */
    @Update("UPDATE user_coupon SET used = 1, used_time = #{usedTime}, order_sn = #{orderSn} " +
            "WHERE id = #{id} AND used = 0")
    int markUsed(@Param("id") Long id, @Param("orderSn") String orderSn, @Param("usedTime") LocalDateTime usedTime);
}
