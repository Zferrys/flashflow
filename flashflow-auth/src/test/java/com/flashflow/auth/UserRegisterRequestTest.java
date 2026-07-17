package com.flashflow.auth;

import com.flashflow.auth.dto.UserRegisterRequest;
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
 * 注册请求参数校验测试
 */
@DisplayName("注册请求参数校验测试")
class UserRegisterRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("合法邮箱+密码+验证码应该通过")
    void validRequestShouldPass() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setEmail("test@example.com");
        req.setVerifyCode("123456");
        req.setPassword("TestPwd@2024");
        req.setNickname("测试用户");
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("非法邮箱格式应该失败")
    void invalidEmailShouldFail() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setEmail("not-an-email");
        req.setVerifyCode("123456");
        req.setPassword("TestPwd@2024");
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("弱密码应该失败")
    void weakPasswordShouldFail() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setEmail("test@example.com");
        req.setVerifyCode("123456");
        req.setPassword("123456"); // 太短，无特殊字符
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("空邮箱应该失败")
    void blankEmailShouldFail() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setEmail("");
        req.setVerifyCode("123456");
        req.setPassword("TestPwd@2024");
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("手机号选填可以不填")
    void phoneOptionalShouldPass() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setEmail("test@example.com");
        req.setVerifyCode("123456");
        req.setPassword("TestPwd@2024");
        // 不设置 phone
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
}
