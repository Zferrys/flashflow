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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final RestTemplate restTemplate;

    /** Redis 订单号自增 Key */
    private static final String ORDER_SN_KEY = "flashflow:order:sn:incr";

    /** Promotion 服务地址 */
    private static final String PROMOTION_BASE = "http://127.0.0.1:8100/api/flashflow/promotion";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrder(OrderInfo order, List<OrderItemDTO> items, Long userCouponId) {
        // 生成订单号
        String orderSn = generateOrderSn();
        order.setOrderSn(orderSn);
        order.setStatus(OrderStatus.PENDING.getCode());

        // 计算商品总额
        BigDecimal total = items.stream()
                .map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        // 优惠券处理：调 promotion 服务计算折扣并核销
        BigDecimal discountAmount = BigDecimal.ZERO;
        Long couponId = null;
        if (userCouponId != null) {
            try {
                // 1. 调 promotion 服务计算折扣（服务端验证，防伪造）
                String calcUrl = PROMOTION_BASE + "/coupon/calculate?userCouponId="
                        + userCouponId + "&amount=" + total;
                var calcResp = restTemplate.exchange(calcUrl, HttpMethod.GET, null,
                        new ParameterizedTypeReference<Map<String, Object>>() {});
                if (calcResp.getBody() != null && calcResp.getBody().get("data") != null) {
                    discountAmount = new BigDecimal(calcResp.getBody().get("data").toString());
                }
                // 2. 折扣大于0才核销
                if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    String markUrl = PROMOTION_BASE + "/coupon/mark-used?userCouponId="
                            + userCouponId + "&orderSn=" + orderSn;
                    var markResp = restTemplate.exchange(markUrl, HttpMethod.POST, null,
                            new ParameterizedTypeReference<Map<String, Object>>() {});
                    if (markResp.getBody() == null || !Boolean.TRUE.equals(markResp.getBody().get("data"))) {
                        log.warn("优惠券核销失败: userCouponId={}", userCouponId);
                        discountAmount = BigDecimal.ZERO; // 核销失败则不使用优惠券
                    }
                }
            } catch (Exception e) {
                log.error("优惠券服务调用异常，降级不使用优惠券: userCouponId={}", userCouponId, e);
                discountAmount = BigDecimal.ZERO;
            }
        }
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(total.subtract(discountAmount));

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

        // 发布 MQ 事件（Saga: 通知库存模块扣减库存，使用实付金额）
        orderEventPublisher.publishOrderCreated(orderSn, order.getUserId(), items, order.getPayAmount());

        log.info("订单创建成功: orderSn={}, total={}, discount={}, pay={}",
                orderSn, total, discountAmount, order.getPayAmount());
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
        // 释放已使用的优惠券
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                String releaseUrl = PROMOTION_BASE + "/coupon/internal/release?orderSn=" + order.getOrderSn();
                restTemplate.postForObject(releaseUrl, null, String.class);
                log.info("取消订单，优惠券已释放: orderSn={}", order.getOrderSn());
            } catch (Exception e) {
                log.error("取消订单释放优惠券失败（优惠券服务不可用）: orderSn={}", order.getOrderSn(), e);
            }
        }
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

        // 获取订单商品明细
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);

        // 发布 MQ 事件：通知库存模块释放库存（Saga 补偿）
        orderEventPublisher.publishOrderRefunded(order.getOrderSn(), items, reason);

        // 同步调用 promotion 服务释放优惠券
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                String releaseUrl = PROMOTION_BASE + "/coupon/internal/release?orderSn=" + order.getOrderSn();
                restTemplate.postForObject(releaseUrl, null, String.class);
                log.info("退款时优惠券已释放: orderSn={}", order.getOrderSn());
            } catch (Exception e) {
                log.error("退款释放优惠券失败（优惠券服务不可用）: orderSn={}", order.getOrderSn(), e);
            }
        }

        log.info("订单已退款: orderSn={}, 库存释放+优惠券释放已触发", order.getOrderSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestRefund(Long orderId, Long userId, String reason) {
        OrderInfo order = getById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能申请退款自己的订单");
        }
        // 只允许从 PAID 或 SHIPPED 申请退款
        if (order.getStatus() != OrderStatus.PAID.getCode()
                && order.getStatus() != OrderStatus.SHIPPED.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持申请退款");
        }
        doTransition(order, OrderStatus.REFUNDING);
        order.setCancelReason(reason);
        orderInfoMapper.updateById(order);
        log.info("退款申请已提交: orderSn={}", order.getOrderSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long orderId) {
        OrderInfo order = getById(orderId);
        doTransition(order, OrderStatus.REFUNDED);
        order.setCancelTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        // 获取订单商品明细
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);

        // 发布 MQ 事件 → 通知库存模块释放库存
        orderEventPublisher.publishOrderRefunded(order.getOrderSn(), items, "管理员审批退款");

        // 释放优惠券
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                String releaseUrl = PROMOTION_BASE + "/coupon/internal/release?orderSn=" + order.getOrderSn();
                restTemplate.postForObject(releaseUrl, null, String.class);
                log.info("退款审批：优惠券已释放: orderSn={}", order.getOrderSn());
            } catch (Exception e) {
                log.error("退款审批释放优惠券失败: orderSn={}", order.getOrderSn(), e);
            }
        }

        recordEvent(order.getId(), order.getOrderSn(), OrderStatus.REFUNDING, OrderStatus.REFUNDED, 2, 0L, "管理员审批通过");
        log.info("退款已审批通过: orderSn={}", order.getOrderSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long orderId, String reason) {
        OrderInfo order = getById(orderId);
        doTransition(order, OrderStatus.PAID); // 回到已支付状态
        order.setCancelReason("退款已拒绝: " + (reason != null ? reason : ""));
        orderInfoMapper.updateById(order);
        recordEvent(order.getId(), order.getOrderSn(), OrderStatus.REFUNDING, OrderStatus.PAID, 2, 0L, reason);
        log.info("退款已拒绝: orderSn={}", order.getOrderSn());
    }

    @Override
    public List<OrderInfo> getRefundPendingList() {
        return orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getStatus, OrderStatus.REFUNDING.getCode())
                .orderByAsc(OrderInfo::getCreateTime));
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
