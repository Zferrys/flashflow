package com.flashflow.promotion.scheduler;

import com.flashflow.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每分钟检查非终态活动，自动根据时间更新状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final PromotionService promotionService;

    @Scheduled(fixedRate = 60_000) // 每 60 秒执行
    public void refreshStatuses() {
        int count = promotionService.refreshExpiredActivities();
        if (count > 0) {
            log.info("定时刷新活动状态完成: {} 个活动状态变更", count);
        }
    }
}
