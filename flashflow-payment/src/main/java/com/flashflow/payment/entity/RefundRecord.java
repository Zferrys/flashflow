package com.flashflow.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录
 */
@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paymentId;

    private String orderSn;

    private BigDecimal refundAmount;

    private String refundReason;

    /** 退款状态 0处理中 1成功 2失败 */
    private Integer refundStatus;

    /** 支付宝退款单号 */
    private String refundNo;

    private Long operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
