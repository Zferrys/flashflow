package com.flashflow.payment.controller;

import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.common.context.UserContext;
import com.flashflow.payment.entity.PaymentOrder;
import com.flashflow.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付控制器
 */
@Tag(name = "支付服务")
@RestController
@RequestMapping("/api/flashflow/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** 管理员权限校验 */
    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @Operation(summary = "发起支付（需登录）")
    @PostMapping("/pay")
    public R<String> pay(@RequestBody PayRequest request) {
        String payUrl = paymentService.createPay(request.orderSn(), request.amount(), request.subject());
        return R.ok(payUrl);
    }

    @Operation(summary = "支付宝异步回调（接收 form-urlencoded 参数，外部接口免鉴权）")
    @PostMapping("/notify")
    public String notify(@RequestParam java.util.Map<String, String> allParams) {
        return paymentService.handleNotify(allParams);
    }

    @Operation(summary = "查询支付状态（需登录）")
    @GetMapping("/{orderSn}/status")
    public R<PaymentOrder> status(@PathVariable String orderSn) {
        return R.ok(paymentService.getPayStatus(orderSn));
    }

    @Operation(summary = "发起退款（管理员）")
    @PostMapping("/refund")
    public R<Void> refund(@RequestBody RefundRequest request) {
        requireAdmin();
        paymentService.refund(request.orderSn(), request.amount(), request.reason());
        return R.ok();
    }

    @Operation(summary = "模拟支付（开发用）")
    @PostMapping("/mock-pay")
    public R<String> mockPay(@RequestParam String orderSn, @RequestParam(required = false) BigDecimal amount) {
        // 检查是否已有支付记录
        PaymentOrder existing = paymentService.getPayStatus(orderSn);
        if (existing == null) {
            // 无支付记录，先创建
            if (amount == null) amount = BigDecimal.ONE;
            paymentService.createPay(orderSn, amount, "模拟支付");
        }
        // 触发支付回调，推进订单状态 PENDING → PAID
        paymentService.handleNotify(java.util.Map.of("out_trade_no", orderSn));
        return R.ok("支付成功");
    }

    record PayRequest(String orderSn, BigDecimal amount, String subject) {}
    record RefundRequest(String orderSn, BigDecimal amount, String reason) {}
}
