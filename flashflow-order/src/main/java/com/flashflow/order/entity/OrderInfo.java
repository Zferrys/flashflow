package com.flashflow.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 */
@Data
@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderSn;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    /** 支付方式 1支付宝 2微信 */
    private Integer payType;

    /** 状态 0待支付 1已支付 2已发货 3已收货 4已完成 5已取消 6退款中 7已退款 */
    private Integer status;

    private Long couponId;

    private BigDecimal discountAmount;

    /** 收货地址快照(JSON) */
    private String addressSnapshot;

    private LocalDateTime paymentTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private LocalDateTime finishTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
}
