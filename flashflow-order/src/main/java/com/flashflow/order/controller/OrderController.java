package com.flashflow.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.common.context.UserContext;
import com.flashflow.common.domain.R;
import com.flashflow.order.entity.OrderEvent;
import com.flashflow.order.entity.OrderInfo;
import com.flashflow.order.entity.OrderItem;
import com.flashflow.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@Slf4j
@Tag(name = "订单服务")
@RestController
@RequestMapping("/api/flashflow/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public R<OrderInfo> create(@RequestBody CreateOrderRequest request) {
        try {
            OrderInfo order = new OrderInfo();
            order.setUserId(UserContext.getUserId());
            order.setRemark(request.remark());
            order.setAddressSnapshot(request.addressSnapshot());
            OrderInfo result = orderService.createOrder(order, request.items());
            return R.ok(result);
        } catch (Exception e) {
            log.error("创建订单失败: userId={}", UserContext.getUserId(), e);
            throw e;
        }
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderInfo> getById(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    @Operation(summary = "按订单号查询")
    @GetMapping("/orderSn/{orderSn}")
    public R<OrderInfo> getByOrderSn(@PathVariable String orderSn) {
        return R.ok(orderService.getByOrderSn(orderSn));
    }

    @Operation(summary = "订单分页")
    @GetMapping("/page")
    public R<IPage<OrderInfo>> page(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) Integer status) {
        return R.ok(orderService.page(new Page<>(page, size), UserContext.getUserId(), status));
    }

    @Operation(summary = "订单商品明细")
    @GetMapping("/{id}/items")
    public R<List<OrderItem>> getItems(@PathVariable Long id) {
        return R.ok(orderService.getItems(id));
    }

    @Operation(summary = "订单事件流水（Event Sourcing）")
    @GetMapping("/{id}/events")
    public R<List<OrderEvent>> getEvents(@PathVariable Long id) {
        return R.ok(orderService.getEvents(id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @RequestParam String reason) {
        orderService.cancel(id, UserContext.getUserId(), reason);
        return R.ok();
    }

    @Operation(summary = "支付成功回调——推进 PENDING → PAID")
    @PostMapping("/{orderSn}/pay-success")
    public R<Void> paySuccess(@PathVariable String orderSn, @RequestParam(defaultValue = "1") Integer payType) {
        orderService.paySuccess(orderSn, payType);
        return R.ok();
    }

    @Operation(summary = "发货——推进 PAID → SHIPPED")
    @PostMapping("/{id}/ship")
    public R<Void> ship(@PathVariable Long id) {
        orderService.ship(id);
        return R.ok();
    }

    @Operation(summary = "确认收货——推进 SHIPPED → DELIVERED")
    @PostMapping("/{id}/deliver")
    public R<Void> deliver(@PathVariable Long id) {
        orderService.confirmDelivery(id);
        return R.ok();
    }

    @Operation(summary = "获取订单总数和统计")
    @GetMapping("/stats")
    public R<OrderService.OrderStats> stats() {
        return R.ok(orderService.getStats());
    }

    // ========== DTO ==========

    record CreateOrderRequest(String remark, String addressSnapshot,
                              List<OrderService.OrderItemDTO> items) {}
}
