package com.flashflow.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存变动日志（审计）
 */
@Data
@TableName("inventory_log")
public class InventoryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

    private Integer shardIndex;

    private Long orderId;

    private String orderSn;

    /** 变动数量（正为扣，负为释放） */
    private Integer quantity;

    /** 变动类型 DEDUCT / RELEASE / CONFIRM */
    private String type;

    private Integer beforeStock;

    private Integer afterStock;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
