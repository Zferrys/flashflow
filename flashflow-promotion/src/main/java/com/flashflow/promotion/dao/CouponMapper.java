package com.flashflow.promotion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.promotion.entity.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CouponMapper extends BaseMapper<Coupon> {

    /** 原子扣减库存（防超卖），返回影响行数 */
    @Update("UPDATE coupon SET remain_count = remain_count - 1 WHERE id = #{id} AND remain_count > 0")
    int decrementRemain(@Param("id") Long id);
}
