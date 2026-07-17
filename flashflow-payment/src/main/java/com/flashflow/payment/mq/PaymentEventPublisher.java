package com.flashflow.payment.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 支付事件发布器（Saga 事务）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final String EXCHANGE = "exchange.payment";

    private final RabbitTemplate rabbitTemplate;

    /** 发布支付成功 → Order: PENDING → PAID */
    public void publishPaymentSuccess(String orderSn, String tradeNo, BigDecimal amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("tradeNo", Objects.toString(tradeNo, ""));
        payload.put("payAmount", amount);
        publish("payment.success", payload, "支付成功: orderSn={}", orderSn);
    }

    /** 发布支付失败 → Order: Saga 补偿 */
    public void publishPaymentFail(String orderSn, String failReason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("failReason", Objects.toString(failReason, ""));
        publish("payment.fail", payload, "支付失败: orderSn={}, reason={}", orderSn, failReason);
    }

    /** 发布退款成功 → Order: status → REFUNDED */
    public void publishRefundSuccess(String orderSn, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("refundReason", Objects.toString(reason, ""));
        publish("payment.refund.success", payload, "退款成功: orderSn={}", orderSn);
    }

    private void publish(String routingKey, Map<String, Object> payload, String logPattern, Object... logArgs) {
        String messageId = UUID.randomUUID().toString();
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload,
                msg -> { msg.getMessageProperties().setMessageId(messageId); return msg; });
        log.info(logPattern, logArgs);
    }
}
