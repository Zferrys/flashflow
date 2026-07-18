package com.flashflow.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class R<T> {

    private int code;
    private String msg;
    private T data;
    private boolean success;

    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, true);
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null, false);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String extraMsg) {
        return new R<>(errorCode.getCode(), errorCode.getMessage() + "：" + extraMsg, null, false);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null, false);
    }

    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS.getCode();
    }
}
