package com.flashflow.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.order.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单 Mapper
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    @Select("SELECT * FROM order_info WHERE order_sn = #{orderSn}")
    OrderInfo selectByOrderSn(@Param("orderSn") String orderSn);
}
