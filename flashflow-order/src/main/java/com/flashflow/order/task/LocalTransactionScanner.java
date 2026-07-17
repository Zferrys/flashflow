package com.flashflow.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashflow.order.dao.LocalTransactionMapper;
import com.flashflow.order.entity.LocalTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Saga 本地事务表定时扫描任务
 * 扫描 INIT 状态的消息重试投递，保证最终一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTransactionScanner {

    private final LocalTransactionMapper localTransactionMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 每分钟扫描一次 */
    @Scheduled(fixedDelay = 60000)
    public void scan() {
        List<LocalTransaction> pendingList = localTransactionMapper.selectList(
                new LambdaQueryWrapper<LocalTransaction>()
                        .eq(LocalTransaction::getStatus, 0)  // INIT
                        .apply("retry_count < max_retry")
                        .orderByAsc(LocalTransaction::getCreateTime)
                        .last("LIMIT 100"));

        for (LocalTransaction tx : pendingList) {
            try {
                // 反序列化 JSON payload 为 Map（确保消费者能正确接收）
                Object payload = tx.getPayload();
                try {
                    if (payload instanceof String) {
                        payload = objectMapper.readValue((String) payload, Map.class);
                    }
                } catch (Exception ignored) { /* keep as-is if not JSON */ }

                rabbitTemplate.convertAndSend(
                        "exchange.order",
                        tx.getBusinessType(),
                        payload,
                        msg -> {
                            msg.getMessageProperties().setMessageId(tx.getMessageId());
                            return msg;
                        });

                // 更新重试次数
                tx.setRetryCount(tx.getRetryCount() + 1);
                tx.setLastRetry(LocalDateTime.now());
                localTransactionMapper.updateById(tx);

                log.info("Saga重试: messageId={}, businessType={}, retry={}",
                        tx.getMessageId(), tx.getBusinessType(), tx.getRetryCount());
            } catch (Exception e) {
                log.error("Saga重试失败: messageId={}", tx.getMessageId(), e);
                // 超过最大重试次数标记为 FAIL
                if (tx.getRetryCount() >= tx.getMaxRetry()) {
                    tx.setStatus(2); // FAIL
                    tx.setRemark("超过最大重试次数");
                    localTransactionMapper.updateById(tx);
                }
            }
        }
    }
}
