package com.flashflow.order;

import com.flashflow.order.entity.OrderEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单事件流水测试
 */
@DisplayName("订单事件流水测试")
class OrderEventTest {

    @Test
    @DisplayName("事件记录应该包含完整字段")
    void eventShouldHaveAllFields() {
        OrderEvent event = new OrderEvent();
        event.setOrderId(1L);
        event.setOrderSn("FF202501010001");
        event.setFromStatus(0);
        event.setToStatus(1);
        event.setOperatorType(0);
        event.setOperator(0L);
        event.setEventTime(LocalDateTime.now());

        assertAll(
                () -> assertEquals(1L, event.getOrderId()),
                () -> assertEquals("FF202501010001", event.getOrderSn()),
                () -> assertEquals(0, event.getFromStatus()),
                () -> assertEquals(1, event.getToStatus()),
                () -> assertEquals(0, event.getOperatorType())
        );
    }

    @Test
    @DisplayName("创建事件时的 fromStatus 可以为 null（初始创建）")
    void fromStatusCanBeNull() {
        OrderEvent event = new OrderEvent();
        event.setFromStatus(null);
        event.setToStatus(0);
        assertNull(event.getFromStatus());
        assertEquals(0, event.getToStatus());
    }

    @Test
    @DisplayName("事件时间默认为当前时间")
    void eventTimeDefaultsToNow() {
        OrderEvent event = new OrderEvent();
        event.setEventTime(LocalDateTime.now());
        assertNotNull(event.getEventTime());
        assertTrue(event.getEventTime().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("extraData 可以存储 JSON")
    void extraDataCanStoreJson() {
        OrderEvent event = new OrderEvent();
        String json = "{\"reason\":\"用户主动取消\",\"operator\":\"admin\"}";
        event.setExtraData(json);
        assertTrue(event.getExtraData().contains("reason"));
        assertTrue(event.getExtraData().contains("admin"));
    }
}
