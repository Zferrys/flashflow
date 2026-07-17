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
    public static final String RK_ORDER_REFUNDED  = "order.refunded";

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
     * 发布订单已退款消息（Saga: 通知库存释放 + 优惠券退回）
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishOrderRefunded(String orderSn, Object items, String reason) {
        String messageId = UUID.randomUUID().toString();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderSn", orderSn);
        payload.put("refundReason", reason != null ? reason : "");
        payload.put("items", items);
        saveAndSend(messageId, "order.refunded", orderSn, payload);
    }

    /**
     * 写本地事务表 + 发 MQ（同一事务）
     *
     * 容错策略：
     *   ① 序列化/DB 写入失败 → 向上抛异常，事务回滚，消息不丢
     *   ② MQ 发送失败 → 记录保持 INIT，由 {@code LocalTransactionScanner} 定时重投
     *   ③ MQ 发送成功但 DB 更新 DONE 失败 → 极低概率，Scanner 会重复投递，消费者幂等兜底
     */
    private void saveAndSend(String messageId, String businessType, String orderSn, Object data) {
        // ── 步骤 1：写本地事务表（序列化失败直接抛，事务回滚）──
        LocalTransaction tx = new LocalTransaction();
        tx.setMessageId(messageId);
        tx.setBusinessType(businessType);
        tx.setBusinessKey(orderSn);
        tx.setStatus(0); // INIT
        tx.setMaxRetry(3);
        tx.setPayload(serialize(data));
        localTransactionMapper.insert(tx);

        // ── 步骤 2：尝试发 MQ ──
        try {
            String routingKey = businessType;
            rabbitTemplate.convertAndSend(EXCHANGE_ORDER, routingKey, data, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                return msg;
            });

            // 发送成功 → 标记 DONE
            tx.setStatus(1);
            localTransactionMapper.updateById(tx);
            log.info("MQ 消息已发送: businessType={}, orderSn={}, messageId={}", businessType, orderSn, messageId);
        } catch (Exception e) {
            // MQ 不可用：记录保持 INIT，由 LocalTransactionScanner 定时重投，不丢消息
            log.error("MQ 发送失败（将由定时任务重投）: businessType={}, orderSn={}", businessType, orderSn, e);
        }
    }

    /** JSON 序列化，失败抛 RuntimeException 触发事务回滚 */
    private String serialize(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("消息序列化失败", e);
        }
    }
}
