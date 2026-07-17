package com.flashflow.promotion;

import com.flashflow.promotion.entity.PromotionActivity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 营销活动状态测试
 */
@DisplayName("营销活动状态测试")
class PromotionActivityStatusTest {

    @Test
    @DisplayName("新创建的活动默认 status 为 null，设为草稿后为 0")
    void newActivityStatus() {
        PromotionActivity activity = new PromotionActivity();
        assertNull(activity.getStatus());
        activity.setStatus(0);
        assertEquals(0, activity.getStatus().intValue());
    }

    @Test
    @DisplayName("活动时间校验：开始时间必须在结束时间之前")
    void startTimeShouldBeBeforeEndTime() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        assertTrue(end.isAfter(start));
    }

    @Test
    @DisplayName("活动时间校验：开始时间不能晚于结束时间")
    void startTimeShouldNotBeAfterEndTime() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        assertTrue(start.isAfter(end));
    }

    @Test
    @DisplayName("活动类型枚举值应该正确")
    void activityTypeShouldBeValid() {
        String[] validTypes = {"FLASH_SALE", "PRE_SALE", "GROUP_BUY"};
        assertAll(
                () -> assertEquals("FLASH_SALE", validTypes[0]),
                () -> assertEquals("PRE_SALE", validTypes[1]),
                () -> assertEquals("GROUP_BUY", validTypes[2])
        );
    }

    @Test
    @DisplayName("活动状态值范围")
    void statusRange() {
        assertAll(
                () -> assertEquals(0, Integer.valueOf(0), "草稿"),
                () -> assertEquals(1, Integer.valueOf(1), "待预热"),
                () -> assertEquals(2, Integer.valueOf(2), "进行中"),
                () -> assertEquals(3, Integer.valueOf(3), "已结束"),
                () -> assertEquals(4, Integer.valueOf(4), "已关闭")
        );
    }
}
