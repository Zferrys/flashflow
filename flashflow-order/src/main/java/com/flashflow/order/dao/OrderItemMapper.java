package com.flashflow.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.order.entity.OrderItem;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(Long orderId);
}
