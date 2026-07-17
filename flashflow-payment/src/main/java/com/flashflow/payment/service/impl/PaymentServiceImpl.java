package com.flashflow.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.payment.dao.PaymentOrderMapper;
import com.flashflow.payment.dao.RefundRecordMapper;
import com.flashflow.payment.entity.PaymentOrder;
import com.flashflow.payment.entity.RefundRecord;
import com.flashflow.payment.mq.PaymentEventPublisher;
import com.flashflow.payment.service.PaymentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付服务实现（支付宝沙箱对接）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alipay.app-id:}")
    private String appId;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    @Value("${alipay.gateway:https://openapi-sandbox.dl.alipaydev.com/gateway.do}")
    private String gatewayUrl;

    @Value("${payment.mock-pay.url:http://127.0.0.1:8120/api/flashflow/payment/mock-pay}")
    private String mockPayUrl;

    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        if (!appId.isEmpty()) {
            this.alipayClient = new DefaultAlipayClient(
                    gatewayUrl, appId, privateKey, "json", "UTF-8",
                    alipayPublicKey, "RSA2");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createPay(String orderSn, BigDecimal amount, String subject) {
        // 检查是否已有支付记录（防重复创建）
        PaymentOrder exist = paymentOrderMapper.selectByOrderSn(orderSn);
        if (exist != null) {
            if (exist.getStatus() == 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "订单已支付，无需重复支付");
            }
            // 已有未支付记录，直接返回
            return buildMockPayUrl(exist);
        }

        // 保存支付记录
        PaymentOrder payOrder = new PaymentOrder();
        payOrder.setOrderSn(orderSn);
        payOrder.setPayAmount(amount);
        payOrder.setPayType(1);
        payOrder.setStatus(0);
        payOrder.setExpireTime(LocalDateTime.now().plusMinutes(30));
        paymentOrderMapper.insert(payOrder);

        // 如果有支付宝配置，调真实接口
        if (alipayClient != null) {
            try {
                AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
                request.setNotifyUrl(notifyUrl);
                // 使用 Map 构造 JSON（安全，防注入）
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("out_trade_no", orderSn);
                bizContent.put("total_amount", amount.toString());
                bizContent.put("subject", subject);
                bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
                request.setBizContent(objectMapper.writeValueAsString(bizContent));

                AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
                if (response.isSuccess()) {
                    log.info("支付宝支付链接生成成功: orderSn={}", orderSn);
                    return response.getBody();
                }
                log.warn("支付宝支付失败: {}", response.getMsg());
            } catch (Exception e) {
                log.error("支付宝调用异常: ", e);
            }
        }

        // 无支付宝配置时，返回模拟支付链接
        return buildMockPayUrl(payOrder);
    }

    private String buildMockPayUrl(PaymentOrder payOrder) {
        log.info("模拟支付: orderSn={}", payOrder.getOrderSn());
        return mockPayUrl + "?orderSn=" + payOrder.getOrderSn();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> notifyParams) {
        log.info("收到支付宝回调: params={}", notifyParams);

        // 1. 验签（防止伪造回调）
        if (alipayClient != null) {
            try {
                boolean signVerified = AlipaySignature.rsaCheckV1(
                        notifyParams, alipayPublicKey, "UTF-8", "RSA2");
                if (!signVerified) {
                    log.error("支付宝回调验签失败: {}", notifyParams);
                    return "fail";
                }
            } catch (AlipayApiException e) {
                log.error("支付宝回调验签异常: ", e);
                return "fail";
            }
        }

        // 2. 解析订单号
        String orderSn = notifyParams.get("out_trade_no");
        if (orderSn == null) {
            log.error("回调缺少 out_trade_no");
            return "fail";
        }

        // 3. 幂等检查（防重复处理）
        PaymentOrder payOrder = paymentOrderMapper.selectByOrderSn(orderSn);
        if (payOrder == null) {
            log.error("支付记录不存在: orderSn={}", orderSn);
            return "fail";
        }
        if (payOrder.getNotifyStatus() == 1) {
            log.info("回调已处理，跳过: orderSn={}", orderSn);
            return "success"; // 已处理，直接返回成功
        }

        // 4. 更新支付状态
        String tradeNo = notifyParams.get("trade_no");
        String totalAmount = notifyParams.get("total_amount");
        payOrder.setTradeNo(tradeNo);
        payOrder.setStatus(1);
        payOrder.setNotifyStatus(1);
        payOrder.setNotifyTime(LocalDateTime.now());
        try {
            payOrder.setNotifyData(objectMapper.writeValueAsString(notifyParams));
        } catch (Exception e) {
            payOrder.setNotifyData(notifyParams.toString());
        }
        paymentOrderMapper.updateById(payOrder);
        log.info("支付回调处理成功: orderSn={}, tradeNo={}", orderSn, tradeNo);

        // 5. 通过 MQ 异步通知订单服务（替代同步 HTTP 调用）
        paymentEventPublisher.publishPaymentSuccess(orderSn, tradeNo,
                totalAmount != null ? new BigDecimal(totalAmount) : payOrder.getPayAmount());

        return "success";
    }

    @Override
    public PaymentOrder getPayStatus(String orderSn) {
        return paymentOrderMapper.selectByOrderSn(orderSn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderSn, BigDecimal amount, String reason) {
        PaymentOrder payOrder = paymentOrderMapper.selectByOrderSn(orderSn);
        if (payOrder == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        // 检查支付状态：只允许已支付的订单退款
        if (payOrder.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_REFUND);
        }
        // 检查退款金额不超过支付金额
        if (amount.compareTo(payOrder.getPayAmount()) > 0) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEED);
        }

        if (alipayClient != null) {
            try {
                AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("out_trade_no", orderSn);
                bizContent.put("refund_amount", amount.toString());
                request.setBizContent(objectMapper.writeValueAsString(bizContent));
                AlipayTradeRefundResponse response = alipayClient.execute(request);
                if (!response.isSuccess()) {
                    throw new BusinessException(ErrorCode.REFUND_FAILED, response.getMsg());
                }
            } catch (AlipayApiException | com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new BusinessException(ErrorCode.REFUND_FAILED, e.getMessage());
            }
        }

        // 记录退款
        RefundRecord record = new RefundRecord();
        record.setPaymentId(payOrder.getId());
        record.setOrderSn(orderSn);
        record.setRefundAmount(amount);
        record.setRefundReason(reason);
        record.setRefundStatus(1);
        refundRecordMapper.insert(record);

        payOrder.setStatus(4); // 已退款
        paymentOrderMapper.updateById(payOrder);
        // 通知订单服务更新状态 → REFUNDED
        paymentEventPublisher.publishRefundSuccess(orderSn, reason);
        log.info("退款成功: orderSn={}, amount={}", orderSn, amount);
    }

}
