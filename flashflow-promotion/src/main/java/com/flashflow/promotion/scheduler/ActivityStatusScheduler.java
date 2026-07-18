package com.flashflow.promotion.scheduler;

import com.flashflow.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 每分钟检查非终态活动，自动根据时间更新状态
 * 使用 Redis 分布式锁防止多实例重复执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final PromotionService promotionService;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "flashflow:scheduler:activity:refresh";

    @Scheduled(fixedRate = 60_000)
    public void refreshStatuses() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        if (!lock.tryLock()) {
            return; // 其他实例正在执行，跳过
        }
        try {
            int count = promotionService.refreshExpiredActivities();
            if (count > 0) {
                log.info("定时刷新活动状态完成: {} 个活动状态变更", count);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
