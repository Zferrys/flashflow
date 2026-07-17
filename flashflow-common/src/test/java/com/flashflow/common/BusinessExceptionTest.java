package com.flashflow.common;

import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常测试
 */
@DisplayName("业务异常测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("通过 ErrorCode 创建异常")
    void shouldCreateWithErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        assertEquals(120001, ex.getCode());
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    @DisplayName("通过 ErrorCode + 附加消息创建异常")
    void shouldCreateWithErrorCodeAndExtra() {
        BusinessException ex = new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "剩余库存: 0");
        assertEquals(130001, ex.getCode());
        assertTrue(ex.getMessage().contains("剩余库存: 0"));
    }

    @Test
    @DisplayName("直接传 code 和 message")
    void shouldCreateWithCodeAndMessage() {
        BusinessException ex = new BusinessException(9999, "直接创建");
        assertEquals(9999, ex.getCode());
        assertEquals("直接创建", ex.getMessage());
    }
}
