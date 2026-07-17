package com.flashflow.common;

import com.flashflow.common.domain.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 错误码测试
 */
@DisplayName("错误码枚举测试")
class ErrorCodeTest {

    @Test
    @DisplayName("所有错误码应该唯一")
    void allCodesShouldBeUnique() {
        ErrorCode[] values = ErrorCode.values();
        long unique = java.util.Arrays.stream(values).mapToInt(ErrorCode::getCode).distinct().count();
        assertEquals(values.length, unique, "错误码必须唯一，不能重复");
    }

    @Test
    @DisplayName("所有错误码应该有非空消息")
    void allMessagesShouldNotBeBlank() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getMessage());
            assertFalse(code.getMessage().isEmpty());
        }
    }

    @Test
    @DisplayName("SUCCESS 的 code 应该为 0")
    void successCodeShouldBeZero() {
        assertEquals(0, ErrorCode.SUCCESS.getCode());
        assertEquals("成功", ErrorCode.SUCCESS.getMessage());
    }

    @Test
    @DisplayName("通用错误码范围应该在 100000-109999")
    void commonErrorCodeRange() {
        assertEquals(100001, ErrorCode.SYSTEM_ERROR.getCode());
        assertEquals(100002, ErrorCode.PARAM_ERROR.getCode());
        assertEquals(100007, ErrorCode.MISSING_REQUEST_BODY.getCode());
    }

    @Test
    @DisplayName("认证错误码范围应该在 110000-119999")
    void authErrorCodeRange() {
        assertEquals(110001, ErrorCode.TOKEN_EXPIRED.getCode());
        assertEquals(110012, ErrorCode.CAPTCHA_ERROR.getCode());
    }

    @Test
    @DisplayName("订单错误码范围应该在 120000-129999")
    void orderErrorCodeRange() {
        assertEquals(120001, ErrorCode.ORDER_NOT_FOUND.getCode());
        assertEquals(120008, ErrorCode.INVALID_ORDER_ITEM.getCode());
    }

    @Test
    @DisplayName("库存错误码范围应该在 130000-139999")
    void inventoryErrorCodeRange() {
        assertEquals(130001, ErrorCode.STOCK_NOT_ENOUGH.getCode());
        assertEquals(130006, ErrorCode.STOCK_RELEASE_FAILED.getCode());
    }

    @Test
    @DisplayName("支付错误码范围应该在 140000-149999")
    void paymentErrorCodeRange() {
        assertEquals(140001, ErrorCode.PAYMENT_FAILED.getCode());
        assertEquals(140007, ErrorCode.REFUND_AMOUNT_EXCEED.getCode());
    }

    @Test
    @DisplayName("营销错误码范围应该在 150000-159999")
    void promotionErrorCodeRange() {
        assertEquals(150001, ErrorCode.ACTIVITY_NOT_STARTED.getCode());
        assertEquals(150008, ErrorCode.PROMOTION_NOT_AVAILABLE.getCode());
    }
}
