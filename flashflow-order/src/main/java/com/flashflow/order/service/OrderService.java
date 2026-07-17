package com.flashflow.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.order.entity.OrderEvent;
import com.flashflow.order.entity.OrderInfo;
import com.flashflow.order.entity.OrderItem;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /** 创建订单 */
    OrderInfo createOrder(OrderInfo order, List<OrderItemDTO> items, Long userCouponId);

    /** 订单分页 */
    IPage<OrderInfo> page(Page<OrderInfo> page, Long userId, Integer status);

    /** 订单详情 */
    OrderInfo getById(Long id);

    /** 订单事件流水 */
    List<OrderEvent> getEvents(Long orderId);

    /** 支付成功——状态机 PENDING → PAID */
    void paySuccess(String orderSn, Integer payType);

    /** 取消订单——状态机 PENDING → CANCELLED */
    void cancel(Long orderId, Long userId, String reason);

    /** 退款完成——状态机 PAID → REFUNDED（从PAID直退，兼容旧流程） */
    void refundSuccess(Long orderId, String reason);

    /** 申请退款（用户）—— PAID → REFUNDING */
    void requestRefund(Long orderId, Long userId, String reason);

    /** 审批退款（管理员）—— REFUNDING → REFUNDED，触发库存释放 */
    void approveRefund(Long orderId);

    /** 拒绝退款（管理员）—— REFUNDING → PAID */
    void rejectRefund(Long orderId, String reason);

    /** 退款中订单列表（管理员） */
    List<OrderInfo> getRefundPendingList();

    /** 确认收货——状态机 SHIPPED → DELIVERED */
    void confirmDelivery(Long orderId);

    /** 发货——状态机 PAID → SHIPPED */
    void ship(Long orderId);

    /** 订单统计 */
    OrderStats getStats();

    /** 获取订单商品明细 */
    List<OrderItem> getItems(Long orderId);

    /** 订单状态直接查询 */
    OrderInfo getByOrderSn(String orderSn);

    /** 订单项 DTO */

    /** 订单统计 DTO */
    record OrderStats(long totalOrders, long paidOrders, long cancelledOrders, String payRate) {}
    record OrderItemDTO(Long skuId, String skuName, String skuImage, Integer quantity, java.math.BigDecimal price) {}
}
