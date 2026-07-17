package com.flashflow.inventory.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashflow.common.mq.MqHelper;
import com.flashflow.inventory.service.InventoryService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 订单事件消费者（Saga 事务补偿）
 *
 *   order.created   → 扣库存（预扣）
 *   order.cancelled → 释放库存
 *   order.paid      → 确认扣减
 *
 * 数据一致性：Redis 幂等 + MqHelper 重试/死信
 */
@Slf4j
@Component
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final MqHelper mqHelper;

    private static final long MAX_RETRY = 3;

    public OrderEventConsumer(InventoryService inventoryService, RedissonClient redissonClient,
                              ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.mqHelper = new MqHelper(redissonClient, "flashflow:idempotent:mq:", MAX_RETRY);
    }

    @RabbitHandler
    @RabbitListener(queues = "queue.inventory.deduct", ackMode = "MANUAL")
    public void handleOrderCreated(Map<String, Object> message, Channel channel, Message rawMessage) throws IOException {
        String messageId = rawMessage.getMessageProperties().getMessageId();
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        if (mqHelper.isDuplicate(messageId)) { channel.basicAck(deliveryTag, false); return; }
        try {
            String orderSn = (String) message.get("orderSn");
            Long userId = Long.valueOf(message.get("userId").toString());
            List<Map<String, Object>> items = objectMapper.convertValue(
                    message.get("items"), new TypeReference<List<Map<String, Object>>>() {});
            log.info("Saga: 扣库存开始, orderSn={}, messageId={}", orderSn, messageId);
            for (Map<String, Object> item : items) {
                Long skuId = Long.valueOf(item.get("skuId").toString());
                Integer quantity = (Integer) item.get("quantity");
                var result = inventoryService.deduct(
                        new InventoryService.DeductRequest(skuId, userId, quantity, orderSn));
                if (!result.success()) {
                    log.error("Saga: 扣库存失败(不重试), orderSn={}, skuId={}", orderSn, skuId);
                    // 业务失败不 requeue：SKU 不存在/库存不足是确定性错误，重试无意义
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }
            mqHelper.markProcessed(messageId);
            channel.basicAck(deliveryTag, false);
            log.info("Saga: 扣库存完成, orderSn={}", orderSn);
        } catch (Exception e) {
            log.error("Saga: 扣库存异常, messageId={}", messageId, e);
            mqHelper.retryOrDead(rawMessage, channel, deliveryTag);
        }
    }

    @RabbitHandler
    @RabbitListener(queues = "queue.inventory.release", ackMode = "MANUAL")
    public void handleOrderCancelled(Map<String, Object> message, Channel channel, Message rawMessage) throws IOException {
        String messageId = rawMessage.getMessageProperties().getMessageId();
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        if (mqHelper.isDuplicate(messageId)) { channel.basicAck(deliveryTag, false); return; }
        try {
            String orderSn = (String) message.get("orderSn");
            List<Map<String, Object>> items = objectMapper.convertValue(
                    message.get("items"), new TypeReference<List<Map<String, Object>>>() {});
            log.info("Saga: 释放库存开始, orderSn={}", orderSn);
            for (Map<String, Object> item : items) {
                Long skuId = Long.valueOf(item.get("skuId").toString());
                Integer quantity = (Integer) item.get("quantity");
                if (!inventoryService.release(skuId, quantity, orderSn)) {
                    // 释放失败不 requeue — Redis key 可能从未存在或被清理，无限重试无意义
                    log.error("Saga: 释放库存失败(跳过), orderSn={}, skuId={}", orderSn, skuId);
                }
            }
            mqHelper.markProcessed(messageId);
            channel.basicAck(deliveryTag, false);
            log.info("Saga: 释放库存完成, orderSn={}", orderSn);
        } catch (Exception e) {
            log.error("Saga: 释放库存异常, messageId={}", messageId, e);
            mqHelper.retryOrDead(rawMessage, channel, deliveryTag);
        }
    }

    @RabbitHandler
    @RabbitListener(queues = "queue.inventory.confirm", ackMode = "MANUAL")
    public void handleOrderPaid(Map<String, Object> message, Channel channel, Message rawMessage) throws IOException {
        String messageId = rawMessage.getMessageProperties().getMessageId();
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        if (mqHelper.isDuplicate(messageId)) { channel.basicAck(deliveryTag, false); return; }
        try {
            String orderSn = (String) message.get("orderSn");
            log.info("Saga: 确认扣减, orderSn={}", orderSn);
            mqHelper.markProcessed(messageId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Saga: 确认扣减异常", e);
            mqHelper.retryOrDead(rawMessage, channel, deliveryTag);
        }
    }
}
