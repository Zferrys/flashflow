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
    @DisplayName("合法手机号+密码应该通过")
    void validRequestShouldPass() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("TestPwd@2024");
        req.setNickname("测试用户");
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("非法手机号格式应该失败")
    void invalidPhoneShouldFail() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setPhone("12345");
        req.setPassword("TestPwd@2024");
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("弱密码应该失败")
    void weakPasswordShouldFail() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("123456"); // 太短，无特殊字符
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("空手机号应该失败")
    void blankPhoneShouldFail() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setPhone("");
        req.setPassword("TestPwd@2024");
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }
}
