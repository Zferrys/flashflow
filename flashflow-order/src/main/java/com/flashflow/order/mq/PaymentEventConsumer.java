package com.flashflow.order.mq;

import com.flashflow.order.dao.OrderInfoMapper;
import com.flashflow.order.entity.OrderInfo;
import com.flashflow.order.service.OrderService;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * 支付事件消费者（Saga 补偿链）
 *
 *   payment.success      → PENDING → PAID
 *   payment.fail         → 自动取消 + 释放库存
 *   payment.refund.success → PAID → REFUNDED
 *
 * 数据一致性：MANUAL ACK + Redis 幂等 + 重试/死信队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;
    private final OrderInfoMapper orderInfoMapper;
    private final RedissonClient redissonClient;

    private static final String IDEMPOTENT_PREFIX = "flashflow:idempotent:mq:";
    private static final long MAX_RETRY = 3;

    @RabbitHandler
    @RabbitListener(queues = "queue.order.paid", containerFactory = "rabbitListenerContainerFactory", concurrency = "5-10")
    public void handlePaymentSuccess(Map<String, Object> message, Channel channel, Message rawMessage) throws IOException {
        AckContext ctx = begin(message, channel, rawMessage);
        if (ctx.skip()) return;
        try {
            String orderSn = (String) message.get("orderSn");
            Integer payType = safeInt(message.get("payType"), 1);
            log.info("Saga: 支付成功, orderSn={}", orderSn);
            orderService.paySuccess(orderSn, payType);
            markProcessed(ctx.messageId);
            channel.basicAck(ctx.deliveryTag, false);
        } catch (Exception e) {
            log.error("Saga: 支付成功处理异常, messageId={}", ctx.messageId, e);
            retryOrDead(rawMessage, channel, ctx.deliveryTag);
        }
    }

    @RabbitHandler
    @RabbitListener(queues = "queue.order.refund.success", containerFactory = "rabbitListenerContainerFactory", concurrency = "3-5")
    public void handleRefundSuccess(Map<String, Object> message, Channel channel, Message rawMessage) throws IOException {
        AckContext ctx = begin(message, channel, rawMessage);
        if (ctx.skip()) return;
        try {
            String orderSn = (String) message.get("orderSn");
            String reason = (String) message.getOrDefault("refundReason", "");
            log.info("退款成功: orderSn={}, reason={}", orderSn, reason);
            OrderInfo order = orderInfoMapper.selectByOrderSn(orderSn);
            if (order != null) {
                orderService.refundSuccess(order.getId(), reason);
            }
            markProcessed(ctx.messageId);
            channel.basicAck(ctx.deliveryTag, false);
        } catch (Exception e) {
            log.error("退款处理异常: messageId={}", ctx.messageId, e);
            retryOrDead(rawMessage, channel, ctx.deliveryTag);
        }
    }

    @RabbitHandler
    @RabbitListener(queues = "queue.order.payment.fail", containerFactory = "rabbitListenerContainerFactory", concurrency = "3-5")
    public void handlePaymentFail(Map<String, Object> message, Channel channel, Message rawMessage) throws IOException {
        AckContext ctx = begin(message, channel, rawMessage);
        if (ctx.skip()) return;
        try {
            String orderSn = (String) message.get("orderSn");
            String failReason = (String) message.getOrDefault("failReason", "支付超时");
            log.warn("Saga: 支付失败，自动取消, orderSn={}, reason={}", orderSn, failReason);
            OrderInfo order = orderInfoMapper.selectByOrderSn(orderSn);
            if (order != null && order.getStatus() == 0) {
                orderService.cancel(order.getId(), order.getUserId(), failReason);
            }
            markProcessed(ctx.messageId);
            channel.basicAck(ctx.deliveryTag, false);
        } catch (Exception e) {
            log.error("Saga: 支付失败处理异常, messageId={}", ctx.messageId, e);
            retryOrDead(rawMessage, channel, ctx.deliveryTag);
        }
    }

    // ══════════ 幂等 + 重试基础设施 ══════════

    private static record AckContext(String messageId, long deliveryTag, boolean skip) {}

    private AckContext begin(Map<?, ?> message, Channel channel, Message rawMessage) throws IOException {
        String messageId = rawMessage.getMessageProperties().getMessageId();
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        if (isDuplicate(messageId)) {
            channel.basicAck(deliveryTag, false);
            return new AckContext(messageId, deliveryTag, true);
        }
        return new AckContext(messageId, deliveryTag, false);
    }

    private boolean isDuplicate(String messageId) {
        return messageId != null && redissonClient.getBucket(IDEMPOTENT_PREFIX + messageId).isExists();
    }

    private void markProcessed(String messageId) {
        if (messageId != null) {
            redissonClient.getBucket(IDEMPOTENT_PREFIX + messageId).set("1", Duration.ofHours(24));
        }
    }

    /** 重试 / 死信：超限 nack 丢弃，否则重新发布 */
    private void retryOrDead(Message rawMessage, Channel channel, long deliveryTag) throws IOException {
        long retryCount = getRetryCount(rawMessage) + 1;
        if (retryCount > MAX_RETRY) {
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        rawMessage.getMessageProperties().setHeader("retry-count", retryCount);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .headers(rawMessage.getMessageProperties().getHeaders())
                .messageId(rawMessage.getMessageProperties().getMessageId())
                .build();
        channel.basicPublish(rawMessage.getMessageProperties().getReceivedExchange(),
                rawMessage.getMessageProperties().getReceivedRoutingKey(), props, rawMessage.getBody());
        channel.basicAck(deliveryTag, false);
    }

    private long getRetryCount(Message message) {
        Object h = message.getMessageProperties().getHeader("retry-count");
        return h instanceof Number n ? n.longValue() : 0;
    }

    private int safeInt(Object val, int defaultVal) {
        return val instanceof Number n ? n.intValue() : defaultVal;
    }
}
