package com.flashflow.promotion.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("product_sku")
public class ProductSku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spuId;
    private String skuName;
    private String specs;
    private BigDecimal price;
    private String image;
    private Integer stock;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
