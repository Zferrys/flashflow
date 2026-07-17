package com.flashflow.order;

import com.flashflow.order.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单状态机测试
 *
 * 合法路径：
 *   PENDING → PAID → SHIPPED → DELIVERED → COMPLETED
 *   PENDING → CANCELLED
 *   PAID → CANCELLED
 *   PAID → REFUNDING → REFUNDED
 */
@DisplayName("订单状态机测试")
class OrderStatusTest {

    @ParameterizedTest(name = "{0} → {1} = {2}")
    @CsvSource({
            "PENDING,  PAID,      true",
            "PENDING,  CANCELLED,  true",
            "PAID,     SHIPPED,    true",
            "PAID,     REFUNDING,  true",
            "PAID,     CANCELLED,  true",
            "SHIPPED,  DELIVERED,  true",
            "DELIVERED, COMPLETED, true",
            "REFUNDING, REFUNDED,  true"
    })
    @DisplayName("合法转换应该通过")
    void shouldAllowValidTransitions(OrderStatus from, OrderStatus to, boolean expected) {
        assertEquals(expected, OrderStatus.canTransition(from, to));
    }

    @ParameterizedTest(name = "{0} → {1} = {2}")
    @CsvSource({
            "PENDING,    SHIPPED,   false",
            "PENDING,    COMPLETED, false",
            "PENDING,    REFUNDED,  false",
            "PAID,       COMPLETED, false",
            "CANCELLED,  PAID,      false",
            "COMPLETED,  PAID,      false",
            "REFUNDED,   PAID,      false",
            "SHIPPED,    CANCELLED, false"
    })
    @DisplayName("非法转换应该拒绝")
    void shouldRejectInvalidTransitions(OrderStatus from, OrderStatus to, boolean expected) {
        assertEquals(expected, OrderStatus.canTransition(from, to));
    }

    @Test
    @DisplayName("终态不能转换到任何状态")
    void terminalStatesShouldNotTransition() {
        assertAll(
                () -> assertFalse(OrderStatus.canTransition(OrderStatus.CANCELLED, OrderStatus.PENDING)),
                () -> assertFalse(OrderStatus.canTransition(OrderStatus.COMPLETED, OrderStatus.PAID)),
                () -> assertFalse(OrderStatus.canTransition(OrderStatus.REFUNDED, OrderStatus.REFUNDING))
        );
    }

    @Test
    @DisplayName("完整正向流程")
    void fullHappyPath() {
        OrderStatus current = OrderStatus.PENDING;
        assertTrue(OrderStatus.canTransition(current, OrderStatus.PAID));
        current = OrderStatus.PAID;
        assertTrue(OrderStatus.canTransition(current, OrderStatus.SHIPPED));
        current = OrderStatus.SHIPPED;
        assertTrue(OrderStatus.canTransition(current, OrderStatus.DELIVERED));
        current = OrderStatus.DELIVERED;
        assertTrue(OrderStatus.canTransition(current, OrderStatus.COMPLETED));
        current = OrderStatus.COMPLETED;
        // COMPLETED 是终态，不能再到任何状态
        assertFalse(OrderStatus.canTransition(current, OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("取消流程：PENDING → CANCELLED")
    void cancelFromPending() {
        assertTrue(OrderStatus.canTransition(OrderStatus.PENDING, OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("退款流程：PAID → REFUNDING → REFUNDED")
    void refundFlow() {
        assertTrue(OrderStatus.canTransition(OrderStatus.PAID, OrderStatus.REFUNDING));
        assertTrue(OrderStatus.canTransition(OrderStatus.REFUNDING, OrderStatus.REFUNDED));
    }

    @Test
    @DisplayName("已支付的订单可以取消")
    void cancelAfterPaid() {
        assertTrue(OrderStatus.canTransition(OrderStatus.PAID, OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("已发货的订单不能取消")
    void cannotCancelAfterShipped() {
        assertFalse(OrderStatus.canTransition(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("fromCode 应该返回正确的枚举")
    void fromCodeShouldReturnCorrectEnum() {
        assertAll(
                () -> assertEquals(OrderStatus.PENDING, OrderStatus.fromCode(0)),
                () -> assertEquals(OrderStatus.PAID, OrderStatus.fromCode(1)),
                () -> assertEquals(OrderStatus.CANCELLED, OrderStatus.fromCode(5)),
                () -> assertEquals(OrderStatus.REFUNDED, OrderStatus.fromCode(7))
        );
    }

    @Test
    @DisplayName("fromCode 对未知 code 应该抛异常")
    void fromCodeShouldThrowForUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromCode(99));
    }
}
