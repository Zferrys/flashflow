package com.flashflow.promotion.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Order 服务熔断器（轻量级滑动窗口实现，无需 Resilience4j 依赖）
 *
 * 状态机：CLOSED → OPEN → HALF_OPEN → CLOSED
 * - CLOSED：正常，请求放行
 * - OPEN：熔断打开，直接拒绝请求
 * - HALF_OPEN：半开，尝试放行一个请求探测是否恢复
 *
 * 触发条件：30 秒窗口内失败率 ≥ 50% 且总请求 ≥ 10
 * 恢复时间：熔断后 15 秒进入半开状态
 */
@Slf4j
@Component
public class OrderServiceBreaker {

    private static final int WINDOW_MS = 30_000;
    private static final int HALF_OPEN_WAIT_MS = 15_000;
    private static final int MIN_REQUESTS = 10;
    private static final double FAILURE_THRESHOLD = 0.5;

    enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state = State.CLOSED;

    // 滑动窗口计数器
    private final AtomicInteger windowRequests = new AtomicInteger(0);
    private final AtomicInteger windowFailures = new AtomicInteger(0);
    private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

    // 熔断开始时间（用于 HALF_OPEN 判断）
    private final AtomicLong openTime = new AtomicLong(0);

    @PostConstruct
    void init() {
        log.info("Order 服务熔断器初始化: 窗口={}s, 阈值={}%, 最小请求数={}, 恢复等待={}s",
                WINDOW_MS / 1000, (int)(FAILURE_THRESHOLD * 100), MIN_REQUESTS, HALF_OPEN_WAIT_MS / 1000);
    }

    /**
     * 是否允许请求通过（线程安全）
     */
    public boolean tryAcquire() {
        State s = state;
        if (s == State.CLOSED) {
            return true;
        }
        if (s == State.OPEN) {
            // 检查是否到半开时间
            if (System.currentTimeMillis() - openTime.get() >= HALF_OPEN_WAIT_MS) {
                if (tryHalfOpen()) {
                    return true; // 半开，放行一个探测请求
                }
            }
            return false;
        }
        // HALF_OPEN：放行探测请求
        return true;
    }

    /** 记录成功（必须在 tryAcquire=true 时调用） */
    public void onSuccess() {
        slideWindow();
        windowRequests.incrementAndGet();
        // 成功会关闭熔断器
        State s = state;
        if (s == State.HALF_OPEN) {
            state = State.CLOSED;
            log.info("Order 熔断器: 探测成功，已关闭");
            resetWindow();
        }
    }

    /** 记录失败（必须在 tryAcquire=true 时调用） */
    public void onFailure() {
        slideWindow();
        windowRequests.incrementAndGet();
        windowFailures.incrementAndGet();
        checkThreshold();
    }

    private synchronized boolean tryHalfOpen() {
        if (state == State.OPEN) {
            state = State.HALF_OPEN;
            log.info("Order 熔断器: 半开，尝试探测");
            return true;
        }
        return false;
    }

    private void checkThreshold() {
        int total = windowRequests.get();
        int failed = windowFailures.get();
        if (total >= MIN_REQUESTS && (double) failed / total >= FAILURE_THRESHOLD) {
            if (state == State.CLOSED || state == State.HALF_OPEN) {
                state = State.OPEN;
                openTime.set(System.currentTimeMillis());
                log.warn("Order 熔断器: 熔断打开！失败率={}/{}={}%，{}秒后尝试恢复",
                        failed, total, failed * 100 / total, HALF_OPEN_WAIT_MS / 1000);
            }
        }
    }

    private void slideWindow() {
        long now = System.currentTimeMillis();
        long start = windowStart.get();
        if (now - start > WINDOW_MS) {
            // 窗口过期，重置
            if (windowStart.compareAndSet(start, now)) {
                windowRequests.set(0);
                windowFailures.set(0);
            }
        }
    }

    private void resetWindow() {
        windowRequests.set(0);
        windowFailures.set(0);
        windowStart.set(System.currentTimeMillis());
    }
}
