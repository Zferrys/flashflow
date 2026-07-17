package com.flashflow.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("cart")
public class Cart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skuId;
    private String skuName;
    private String skuImage;
    private BigDecimal price;
    private Integer quantity;
    private Integer checked;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
