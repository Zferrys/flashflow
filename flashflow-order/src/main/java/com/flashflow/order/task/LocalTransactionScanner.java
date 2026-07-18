package com.flashflow.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashflow.order.dao.LocalTransactionMapper;
import com.flashflow.order.entity.LocalTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Saga 本地事务表定时扫描任务（分布式锁防多实例重复执行）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTransactionScanner {

    private final LocalTransactionMapper localTransactionMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LOCK_KEY = "flashflow:scheduler:tx:scan";

    /** 每分钟扫描一次 */
    @Scheduled(fixedDelay = 60000)
    public void scan() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        if (!lock.tryLock()) {
            return; // 其他实例正在执行
        }
        try {
            List<LocalTransaction> pendingList = localTransactionMapper.selectList(
                    new LambdaQueryWrapper<LocalTransaction>()
                            .eq(LocalTransaction::getStatus, 0)
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
                if (tx.getRetryCount() >= tx.getMaxRetry()) {
                    tx.setStatus(2); // FAIL
                    tx.setRemark("超过最大重试次数");
                    localTransactionMapper.updateById(tx);
                }
            }
        }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
