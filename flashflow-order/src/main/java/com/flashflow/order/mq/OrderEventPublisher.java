package com.flashflow.order.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashflow.order.dao.LocalTransactionMapper;
import com.flashflow.order.entity.LocalTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 订单事件发布器（Saga 事务消息）
 *
 * 保证机制：
 *   ① 业务操作 + local_transaction 在同一 @Transactional
 *   ② 定时任务扫描 INIT 状态消息重投
 *   ③ 消费者侧 message_id 唯一索引幂等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final LocalTransactionMapper localTransactionMapper;
    private final ObjectMapper objectMapper;

    /** 交换机 */
    public static final String EXCHANGE_ORDER = "exchange.order";

    /** 路由键 */
    public static final String RK_ORDER_CREATED   = "order.created";
    public static final String RK_ORDER_PAID      = "order.paid";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";

    /**
     * 发布订单已创建消息（事务保证）
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishOrderCreated(String orderSn, Long userId, Object items, java.math.BigDecimal totalAmount) {
        String messageId = UUID.randomUUID().toString();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("userId", userId);
        payload.put("items", items);
        payload.put("totalAmount", totalAmount);
        saveAndSend(messageId, "order.created", orderSn, payload);
    }

    /**
     * 发布订单已支付消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishOrderPaid(String orderSn, String tradeNo, java.math.BigDecimal payAmount) {
        String messageId = UUID.randomUUID().toString();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("tradeNo", tradeNo != null ? tradeNo : "");
        payload.put("payAmount", payAmount);
        saveAndSend(messageId, "order.paid", orderSn, payload);
    }

    /**
     * 发布订单已取消消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishOrderCancelled(String orderSn, String reason, Object items) {
        String messageId = UUID.randomUUID().toString();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("cancelReason", reason != null ? reason : "");
        payload.put("items", items);
        saveAndSend(messageId, "order.cancelled", orderSn, payload);
    }

    /**
     * 写本地事务表 + 发 MQ（同一事务）
     */
    private void saveAndSend(String messageId, String businessType, String orderSn, Object data) {
        try {
            // 写入本地事务表
            LocalTransaction tx = new LocalTransaction();
            tx.setMessageId(messageId);
            tx.setBusinessType(businessType);
            tx.setBusinessKey(orderSn);
            tx.setStatus(0); // INIT
            tx.setPayload(objectMapper.writeValueAsString(data));
            tx.setMaxRetry(3);
            localTransactionMapper.insert(tx);

            // 发送 MQ 消息
            String routingKey = businessType;
            rabbitTemplate.convertAndSend(EXCHANGE_ORDER, routingKey, data, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                return msg;
            });

            // 更新状态为 DONE
            tx.setStatus(1);
            localTransactionMapper.updateById(tx);

            log.info("MQ 消息已发送: businessType={}, orderSn={}, messageId={}", businessType, orderSn, messageId);
        } catch (Exception e) {
            log.error("MQ 消息发送失败: businessType={}, orderSn={}", businessType, orderSn, e);
        }
    }
}
