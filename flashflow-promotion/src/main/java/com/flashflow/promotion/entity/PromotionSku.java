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

    @jakarta.validation.constraints.NotNull(message = "活动ID不能为空")
    private Long activityId;

    private Long skuId;

    @jakarta.validation.constraints.NotBlank(message = "商品名称不能为空")
    private String skuName;

    private String skuImage;

    @jakarta.validation.constraints.NotNull(message = "原价不能为空")
    private BigDecimal originalPrice;

    @jakarta.validation.constraints.NotNull(message = "秒杀价不能为空")
    private BigDecimal activityPrice;

    /** 活动库存上限 */
    @jakarta.validation.constraints.NotNull(message = "库存不能为空")
    @jakarta.validation.constraints.Min(value = 1, message = "库存必须大于0")
    private Integer stockLimit;

    /** 每人限购 */
    @jakarta.validation.constraints.NotNull(message = "限购不能为空")
    @jakarta.validation.constraints.Min(value = 1, message = "限购至少为1")
    private Integer perUserLimit;

    private Integer soldCount;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
