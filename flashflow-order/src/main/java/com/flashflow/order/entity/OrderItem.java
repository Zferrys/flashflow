package com.flashflow.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderSn;

    private Long skuId;

    private String skuName;

    private String skuImage;

    private BigDecimal skuPrice;

    private Integer quantity;

    private BigDecimal subTotal;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
