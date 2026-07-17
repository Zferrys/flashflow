package com.flashflow.promotion.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer totalCount;
    private Integer remainCount;
    private Integer perUserLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    /** 适用范围: ALL=全场, CATEGORY=分类, SKU=指定商品 */
    private String scope;
    /** 范围值: 分类ID 或 SKU ID JSON数组 */
    private String scopeValue;
    /** 适用SKU_ID（预留，未在DB建列，使用 exist=false 避免查询报错） */
    @TableField(exist = false)
    private Long scopeSkuId;
    /** 自动发放: NONE=手动领取, NEW_USER=新用户, FIRST_ORDER=首单 */
    private String autoGrant;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    // ========== 常量 ==========

    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_CATEGORY = "CATEGORY";
    public static final String SCOPE_SKU = "SKU";

    public static final String GRANT_NONE = "NONE";
    public static final String GRANT_NEW_USER = "NEW_USER";
    public static final String GRANT_FIRST_ORDER = "FIRST_ORDER";

    public static final int TYPE_FIXED = 1;
    public static final int TYPE_DISCOUNT = 2;
}
