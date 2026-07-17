package com.flashflow.payment.service;

import com.flashflow.payment.entity.PaymentOrder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /** 发起支付（返回支付宝支付链接） */
    String createPay(String orderSn, BigDecimal amount, String subject);

    /** 支付宝异步回调处理（接收验签后的参数 Map） */
    String handleNotify(Map<String, String> notifyParams);

    /** 查询支付状态 */
    PaymentOrder getPayStatus(String orderSn);

    /** 发起退款 */
    void refund(String orderSn, BigDecimal amount, String reason);
}
