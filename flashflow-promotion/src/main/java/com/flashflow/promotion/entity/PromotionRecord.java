package com.flashflow.promotion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动参与记录（防重复 + 限购校验）
 */
@Data
@TableName("promotion_record")
public class PromotionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long skuId;

    private Long userId;

    private Long orderId;

    private Integer quantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
