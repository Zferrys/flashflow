package com.flashflow.promotion.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("user_coupon")
public class UserCoupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer used;
    private LocalDateTime usedTime;
    private String orderSn;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime getTime;
}
