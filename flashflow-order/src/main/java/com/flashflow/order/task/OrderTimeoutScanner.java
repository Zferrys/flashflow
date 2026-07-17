package com.flashflow.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashflow.order.dao.OrderInfoMapper;
import com.flashflow.order.entity.OrderInfo;
import com.flashflow.order.entity.OrderStatus;
import com.flashflow.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 * 每分钟扫描超过30分钟未支付的PENDING订单，自动取消并释放库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScanner {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderService orderService;

    /** 支付超时时间（分钟） */
    private static final int PAY_TIMEOUT_MINUTES = 30;
    /** 每次扫描最多处理数 */
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 60000) // 每分钟执行
    public void cancelTimeoutOrders() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(PAY_TIMEOUT_MINUTES);

        List<OrderInfo> expiredOrders = orderInfoMapper.selectList(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getStatus, OrderStatus.PENDING.getCode())
                        .lt(OrderInfo::getCreateTime, timeout)
                        .last("LIMIT " + BATCH_SIZE));

        if (expiredOrders.isEmpty()) return;

        int cancelled = 0;
        for (OrderInfo order : expiredOrders) {
            try {
                // cancel 内部会：校验状态机 → 更新状态 → 发布 order.cancelled MQ → Inventory 释放库存
                orderService.cancel(order.getId(), order.getUserId(), "订单超时自动取消");
                cancelled++;
            } catch (Exception e) {
                log.warn("超时取消订单失败: orderSn={}, reason={}", order.getOrderSn(), e.getMessage());
            }
        }

        if (cancelled > 0) {
            log.info("超时取消订单完成: 扫描{}条, 取消{}条", expiredOrders.size(), cancelled);
        }
    }
}
