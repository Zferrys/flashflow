package com.flashflow.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.order.entity.OrderEvent;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单事件 Mapper
 */
public interface OrderEventMapper extends BaseMapper<OrderEvent> {

    @Select("SELECT * FROM order_event WHERE order_id = #{orderId} ORDER BY event_time ASC")
    List<OrderEvent> selectByOrderId(Long orderId);
}
