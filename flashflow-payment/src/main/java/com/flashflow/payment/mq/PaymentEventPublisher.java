package com.flashflow.payment.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 支付事件发布器
 *
 * MQ 发送失败时抛出 AmqpException，由调用方的事务回滚保证数据一致性
 * （PaymentServiceImpl.handleNotify 内 @Transactional 会回滚 payment_order 状态更新）
 */
@Slf4j
@Component
public class PaymentEventPublisher {

    private static final String EXCHANGE = "exchange.payment";

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** 发布支付成功 → Order: PENDING → PAID */
    public void publishPaymentSuccess(String orderSn, String tradeNo, BigDecimal amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("tradeNo", Objects.toString(tradeNo, ""));
        payload.put("payAmount", amount);
        publish("payment.success", payload);
        log.info("支付成功事件已发布: orderSn={}", orderSn);
    }

    /** 发布支付失败 → Order: Saga 补偿 */
    public void publishPaymentFail(String orderSn, String failReason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("failReason", Objects.toString(failReason, ""));
        publish("payment.fail", payload);
        log.info("支付失败事件已发布: orderSn={}, reason={}", orderSn, failReason);
    }

    /** 发布退款成功 → Order: status → REFUNDED */
    public void publishRefundSuccess(String orderSn, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("refundReason", Objects.toString(reason, ""));
        publish("payment.refund.success", payload);
        log.info("退款成功事件已发布: orderSn={}", orderSn);
    }

    private void publish(String routingKey, Map<String, Object> payload) {
        String messageId = UUID.randomUUID().toString();
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload,
                msg -> { msg.getMessageProperties().setMessageId(messageId); return msg; });
    }
}
