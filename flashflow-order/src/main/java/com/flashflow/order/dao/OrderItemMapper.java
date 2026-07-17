package com.flashflow.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.order.entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(Long orderId);

    @Insert("<script>" +
            "INSERT INTO order_item (order_id, order_sn, sku_id, sku_name, sku_image, sku_price, quantity, sub_total) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.orderId}, #{item.orderSn}, #{item.skuId}, #{item.skuName}, #{item.skuImage}, #{item.skuPrice}, #{item.quantity}, #{item.subTotal})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<OrderItem> list);
}
