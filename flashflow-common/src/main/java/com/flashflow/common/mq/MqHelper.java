package com.flashflow.common.mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.time.Duration;

/**
 * MQ 消费者通用辅助：幂等检测 + 重试/死信发布。
 * 所有消费者通过构造函数注入 RedissonClient + 前缀 + 最大重试次数。
 */
public final class MqHelper {

    private static final Duration IDEMPOTENT_TTL = Duration.ofHours(24);

    private final RedissonClient redissonClient;
    private final String idempotentPrefix;
    private final long maxRetry;

    public MqHelper(RedissonClient redissonClient, String idempotentPrefix, long maxRetry) {
        this.redissonClient = redissonClient;
        this.idempotentPrefix = idempotentPrefix;
        this.maxRetry = maxRetry;
    }

    /** 幂等检查：已处理过 → 返回 true */
    public boolean isDuplicate(String messageId) {
        return messageId != null && redissonClient.getBucket(idempotentPrefix + messageId).isExists();
    }

    /** 标记已处理 */
    public void markProcessed(String messageId) {
        if (messageId != null) {
            redissonClient.getBucket(idempotentPrefix + messageId).set("1", IDEMPOTENT_TTL);
        }
    }

    /** 重试/死信：超限 nack 丢弃，否则重新发布并 ack */
    public void retryOrDead(Message rawMessage, Channel channel, long deliveryTag) throws IOException {
        long retryCount = getRetryCount(rawMessage) + 1;
        if (retryCount > maxRetry) {
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        rawMessage.getMessageProperties().setHeader("retry-count", retryCount);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .headers(rawMessage.getMessageProperties().getHeaders())
                .messageId(rawMessage.getMessageProperties().getMessageId())
                .build();
        channel.basicPublish(rawMessage.getMessageProperties().getReceivedExchange(),
                rawMessage.getMessageProperties().getReceivedRoutingKey(),
                props, rawMessage.getBody());
        channel.basicAck(deliveryTag, false);
    }

    private long getRetryCount(Message message) {
        Object h = message.getMessageProperties().getHeader("retry-count");
        return h instanceof Number n ? n.longValue() : 0;
    }
}
