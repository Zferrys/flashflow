package com.flashflow.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单
 */
@Data
@TableName("payment_order")
public class PaymentOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderSn;

    /** 支付宝交易号 */
    private String tradeNo;

    private BigDecimal payAmount;

    /** 支付方式 1支付宝 */
    private Integer payType;

    /** 状态 0待支付 1支付成功 2支付失败 3退款中 4已退款 */
    private Integer status;

    /** 回调状态 0未收到 1已收到 2验签失败 */
    private Integer notifyStatus;

    /** 回调原始数据(JSON) */
    private String notifyData;

    private LocalDateTime notifyTime;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
