package com.flashflow.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单事件流水（Event Sourcing）
 */
@Data
@TableName("order_event")
public class OrderEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderSn;

    /** 原状态 */
    private Integer fromStatus;

    /** 目标状态 */
    private Integer toStatus;

    /** 操作人ID */
    private Long operator;

    /** 操作人类型 0系统 1用户 2管理员 */
    private Integer operatorType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime eventTime;

    /** 扩展数据(JSON) */
    private String extraData;
}
