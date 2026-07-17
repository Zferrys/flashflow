package com.flashflow.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.order.entity.OrderInfo;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 订单 Mapper
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    @Select("SELECT id, order_sn, user_id, total_amount, pay_amount, discount_amount, pay_type, status, coupon_id, address_snapshot, payment_time, cancel_time, cancel_reason, finish_time, remark, create_time, update_time, is_deleted FROM order_info WHERE order_sn = #{orderSn}")
    OrderInfo selectByOrderSn(@Param("orderSn") String orderSn);

    /** 聚合统计：一次查询返回各状态订单数 */
    @Select("SELECT COALESCE(status, -1) as s, COUNT(*) as c FROM order_info GROUP BY status")
    @MapKey("s")
    Map<Integer, Map<String, Object>> countByStatus();
}
