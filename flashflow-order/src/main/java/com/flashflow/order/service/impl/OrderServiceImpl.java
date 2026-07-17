package com.flashflow.order.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.order.dao.OrderEventMapper;
import com.flashflow.order.dao.OrderInfoMapper;
import com.flashflow.order.dao.OrderItemMapper;
import com.flashflow.order.entity.OrderEvent;
import com.flashflow.order.entity.OrderInfo;
import com.flashflow.order.entity.OrderItem;
import com.flashflow.order.entity.OrderStatus;
import com.flashflow.order.mq.OrderEventPublisher;
import com.flashflow.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单服务实现（状态机 + Event Sourcing）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderEventMapper orderEventMapper;
    private final RedissonClient redissonClient;
    private final OrderEventPublisher orderEventPublisher;

    /** Redis 订单号自增 Key */
    private static final String ORDER_SN_KEY = "flashflow:order:sn:incr";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrder(OrderInfo order, List<OrderItemDTO> items) {
        // 生成订单号
        String orderSn = generateOrderSn();
        order.setOrderSn(orderSn);
        order.setStatus(OrderStatus.PENDING.getCode());

        // 计算金额
        BigDecimal total = items.stream()
                .map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        order.setPayAmount(total);

        // 保存订单
        orderInfoMapper.insert(order);

        // 保存订单项
        for (OrderItemDTO item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderSn(orderSn);
            orderItem.setSkuId(item.skuId());
            orderItem.setSkuName(item.skuName());
            orderItem.setSkuImage(item.skuImage());
            orderItem.setSkuPrice(item.price());
            orderItem.setQuantity(item.quantity());
            orderItem.setSubTotal(item.price().multiply(BigDecimal.valueOf(item.quantity())));
            orderItemMapper.insert(orderItem);
        }

        // 记录事件
        recordEvent(order.getId(), orderSn, null, OrderStatus.PENDING, 0, 0L, null);

        // 发布 MQ 事件（Saga: 通知库存模块扣减库存）
        orderEventPublisher.publishOrderCreated(orderSn, order.getUserId(), items, total);

        log.info("订单创建成功: orderSn={}, amount={}", orderSn, total);
        return order;
    }

    @Override
    public IPage<OrderInfo> page(Page<OrderInfo> page, Long userId, Integer status) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(OrderInfo::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return orderInfoMapper.selectPage(page, wrapper);
    }

    @Override
    public OrderInfo getById(Long id) {
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public List<OrderEvent> getEvents(Long orderId) {
        return orderEventMapper.selectByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderSn, Integer payType) {
        OrderInfo order = getByOrderSn(orderSn);
        doTransition(order, OrderStatus.PAID);
        order.setPayType(payType);
        order.setPaymentTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        // 发布 MQ 事件（Saga: 通知库存模块确认扣减）
        orderEventPublisher.publishOrderPaid(orderSn, null, order.getPayAmount());
        log.info("订单支付成功: orderSn={}", orderSn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId, String reason) {
        OrderInfo order = getById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能取消自己的订单");
        }
        doTransition(order, OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderInfoMapper.updateById(order);
        // 发布 MQ 事件（Saga: 通知库存模块释放库存）
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        orderEventPublisher.publishOrderCancelled(order.getOrderSn(), reason, items);
        log.info("订单已取消: orderSn={}", order.getOrderSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundSuccess(Long orderId, String reason) {
        OrderInfo order = getById(orderId);
        doTransition(order, OrderStatus.REFUNDED);
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        recordEvent(order.getId(), order.getOrderSn(), null, OrderStatus.REFUNDED, 0, 0L, reason);
        log.info("订单已退款: orderSn={}", order.getOrderSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long orderId) {
        OrderInfo order = getById(orderId);
        doTransition(order, OrderStatus.SHIPPED);
        orderInfoMapper.updateById(order);
        log.info("发货: orderSn={}", order.getOrderSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDelivery(Long orderId) {
        OrderInfo order = getById(orderId);
        doTransition(order, OrderStatus.DELIVERED);
        orderInfoMapper.updateById(order);
        log.info("确认收货: orderSn={}", order.getOrderSn());
    }

    @Override
    public OrderStats getStats() {
        long total = orderInfoMapper.selectCount(null);
        long paid = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getStatus, OrderStatus.PAID.getCode()));
        long cancelled = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getStatus, OrderStatus.CANCELLED.getCode()));
        String payRate = total > 0 ? String.format("%.1f%%", paid * 100.0 / total) : "0%";
        return new OrderStats(total, paid, cancelled, payRate);
    }

    @Override
    public List<OrderItem> getItems(Long orderId) {
        return orderItemMapper.selectByOrderId(orderId);
    }

    @Override
    public OrderInfo getByOrderSn(String orderSn) {
        OrderInfo order = orderInfoMapper.selectByOrderSn(orderSn);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    // ========== 私有方法 ==========

    /** 状态机转换（核心） */
    private void doTransition(OrderInfo order, OrderStatus target) {
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (!OrderStatus.canTransition(current, target)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR,
                    String.format("不允许从 %s 转换到 %s", current.getDesc(), target.getDesc()));
        }
        order.setStatus(target.getCode());
        recordEvent(order.getId(), order.getOrderSn(), current, target, 0, 0L, null);
    }

    /** 记录事件流水 */
    private void recordEvent(Long orderId, String orderSn, OrderStatus from, OrderStatus to,
                             int operatorType, Long operator, String extraData) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setOrderSn(orderSn);
        event.setFromStatus(from != null ? from.getCode() : null);
        event.setToStatus(to.getCode());
        event.setOperatorType(operatorType);
        event.setOperator(operator);
        event.setExtraData(extraData);
        orderEventMapper.insert(event);
    }

    /**
     * 生成订单号：FF + yyyyMMddHHmmss + 6位Redis原子自增序列
     * 使用 Redis INCR 保证并发安全，每天自动重置
     */
    private String generateOrderSn() {
        String todayKey = ORDER_SN_KEY + ":" + DateUtil.format(LocalDateTime.now(), "yyyyMMdd");
        RAtomicLong atomicLong = redissonClient.getAtomicLong(todayKey);
        // 首次使用设置过期时间（当天结束时过期）
        if (!atomicLong.isExists()) {
            atomicLong.set(0);
            atomicLong.expire(java.time.Duration.ofDays(1));
        }
        long seq = atomicLong.incrementAndGet();
        return "FF" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") +
                String.format("%06d", seq % 1000000);
    }
}
