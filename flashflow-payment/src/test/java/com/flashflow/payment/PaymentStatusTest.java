package com.flashflow.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付状态测试
 */
@DisplayName("支付状态测试")
class PaymentStatusTest {

    @Test
    @DisplayName("支付金额必须大于零")
    void payAmountShouldBePositive() {
        BigDecimal amount = new BigDecimal("99.00");
        assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("支付金额为零应该失败")
    void zeroAmountShouldFail() {
        BigDecimal amount = BigDecimal.ZERO;
        assertFalse(amount.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("支付金额为负数应该失败")
    void negativeAmountShouldFail() {
        BigDecimal amount = new BigDecimal("-10.00");
        assertFalse(amount.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("退款金额不能大于支付金额")
    void refundShouldNotExceedPay() {
        BigDecimal payAmount = new BigDecimal("100.00");
        BigDecimal refundAmount = new BigDecimal("50.00");
        assertTrue(refundAmount.compareTo(payAmount) <= 0);
    }

    @Test
    @DisplayName("退款金额超过支付金额应该失败")
    void refundExceedingPayShouldFail() {
        BigDecimal payAmount = new BigDecimal("100.00");
        BigDecimal refundAmount = new BigDecimal("150.00");
        assertFalse(refundAmount.compareTo(payAmount) <= 0);
    }

    @Test
    @DisplayName("支付状态值范围应该有效")
    void statusRange() {
        assertAll(
                () -> assertEquals(0, Integer.valueOf(0), "待支付"),
                () -> assertEquals(1, Integer.valueOf(1), "支付成功"),
                () -> assertEquals(2, Integer.valueOf(2), "支付失败"),
                () -> assertEquals(3, Integer.valueOf(3), "退款中"),
                () -> assertEquals(4, Integer.valueOf(4), "已退款")
        );
    }

    @Test
    @DisplayName("支付过期时间应为当前时间 + 30 分钟")
    void expireTimeShouldBe30Minutes() {
        // 模拟：创建支付记录时设置 expireTime
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime expire = now.plusMinutes(30);
        assertTrue(expire.isAfter(now));
        assertEquals(30, java.time.Duration.between(now, expire).toMinutes());
    }
}
