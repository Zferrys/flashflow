package com.flashflow.common;

import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一响应体 R<T> 测试
 */
@DisplayName("统一响应体测试")
class RTest {

    @Test
    @DisplayName("ok() 应该返回成功的响应")
    void okShouldReturnSuccess() {
        R<String> result = R.ok("hello");
        assertEquals(0, result.getCode());
        assertEquals("成功", result.getMsg());
        assertEquals("hello", result.getData());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("ok() 无参数应该返回 data=null")
    void okNoArgShouldReturnNullData() {
        R<Void> result = R.ok();
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail() 应该返回错误响应")
    void failShouldReturnError() {
        R<Void> result = R.fail(ErrorCode.STOCK_NOT_ENOUGH);
        assertEquals(130001, result.getCode());
        assertEquals("库存不足", result.getMsg());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("fail() 带额外消息应该拼接")
    void failWithExtraMsgShouldConcatenate() {
        R<Void> result = R.fail(ErrorCode.PARAM_ERROR, "数量不能为负数");
        assertEquals(100002, result.getCode());
        assertTrue(result.getMsg().contains("数量不能为负数"));
    }

    @Test
    @DisplayName("fail() 直接传 code+msg 应该返回自定义错误")
    void failWithCodeAndMsg() {
        R<Void> result = R.fail(999, "自定义错误");
        assertEquals(999, result.getCode());
        assertEquals("自定义错误", result.getMsg());
    }
}
