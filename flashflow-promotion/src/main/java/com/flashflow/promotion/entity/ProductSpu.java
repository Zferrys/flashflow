package com.flashflow.promotion.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("product_spu")
public class ProductSpu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String spuName;
    private Long categoryId;
    private String description;
    private String mainImage;
    private String images;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
