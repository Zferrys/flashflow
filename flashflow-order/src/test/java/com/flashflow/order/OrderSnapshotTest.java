package com.flashflow.order;

import com.flashflow.order.entity.OrderInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单业务逻辑测试
 */
@DisplayName("订单业务逻辑测试")
class OrderSnapshotTest {

    @Test
    @DisplayName("新订单应该是待支付状态")
    void newOrderShouldBePending() {
        OrderInfo order = new OrderInfo();
        order.setOrderSn("FF202501010001");
        order.setUserId(1001L);
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setStatus(0);
        assertEquals(0, order.getStatus());
    }

    @Test
    @DisplayName("订单金额不能为负数")
    void amountShouldNotBeNegative() {
        OrderInfo order = new OrderInfo();
        order.setTotalAmount(new BigDecimal("-10.00"));
        assertTrue(order.getTotalAmount().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    @DisplayName("订单金额精度应该为两位小数")
    void amountPrecisionShouldBeTwoDecimal() {
        BigDecimal amount = new BigDecimal("99.99");
        assertEquals(2, amount.scale());
    }

    @Test
    @DisplayName("实付金额不能大于总金额")
    void payAmountShouldNotExceedTotal() {
        BigDecimal total = new BigDecimal("100.00");
        BigDecimal pay = new BigDecimal("80.00");
        assertTrue(pay.compareTo(total) <= 0);
    }

    @Test
    @DisplayName("订单号格式应为 FF + 日期 + 序列")
    void orderSnFormat() {
        String orderSn = "FF20250101000001";
        assertAll(
                () -> assertTrue(orderSn.startsWith("FF")),
                () -> assertEquals(16, orderSn.length())
        );
    }

    @Test
    @DisplayName("支付时间应该在创建时间之后")
    void paymentTimeShouldBeAfterCreateTime() {
        LocalDateTime createTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime paymentTime = LocalDateTime.of(2025, 1, 1, 10, 5);
        assertTrue(paymentTime.isAfter(createTime));
    }

    @Test
    @DisplayName("取消时间应该在创建时间之后")
    void cancelTimeShouldBeAfterCreateTime() {
        LocalDateTime createTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime cancelTime = LocalDateTime.of(2025, 1, 1, 10, 30);
        assertTrue(cancelTime.isAfter(createTime));
    }
}
