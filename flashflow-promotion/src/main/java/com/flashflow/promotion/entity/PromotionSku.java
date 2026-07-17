package com.flashflow.promotion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动商品
 */
@Data
@TableName("promotion_sku")
public class PromotionSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long skuId;

    private String skuName;

    private String skuImage;

    private BigDecimal originalPrice;

    private BigDecimal activityPrice;

    /** 活动库存上限 */
    private Integer stockLimit;

    /** 每人限购 */
    private Integer perUserLimit;

    private Integer soldCount;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
