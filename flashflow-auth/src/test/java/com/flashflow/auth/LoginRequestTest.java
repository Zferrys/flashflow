package com.flashflow.auth;

import com.flashflow.auth.dto.LoginRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录请求参数校验测试
 */
@DisplayName("登录请求参数校验测试")
class LoginRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("合法参数应该通过校验")
    void validRequestShouldPass() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("TestPwd@2024");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("空用户名应该校验失败")
    void blankUsernameShouldFail() {
        LoginRequest req = new LoginRequest();
        req.setUsername("");
        req.setPassword("TestPwd@2024");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("用户名")));
    }

    @Test
    @DisplayName("空密码应该校验失败")
    void blankPasswordShouldFail() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("密码")));
    }
}
