package com.flashflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 登录 API 集成测试
 * 启动完整 Spring 上下文，验证登录接口通不通
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("管理员登录应该返回 Token")
    void adminLoginShouldReturnToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "account", "admin",
                "password", "Admin@123"
        ));

        mockMvc.perform(post("/api/flashflow/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andDo(result -> {
                    String response = result.getResponse().getContentAsString();
                    System.out.println("登录响应: " + response);
                });
    }

    @Test
    @Order(2)
    @DisplayName("错误密码应该返回 LOGIN_FAILED")
    void wrongPasswordShouldFail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "account", "admin",
                "password", "wrong-password"
        ));

        mockMvc.perform(post("/api/flashflow/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110006));
    }

    @Test
    @Order(3)
    @DisplayName("空参数应该返回 PARAM_ERROR")
    void blankParamsShouldFail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "account", "",
                "password", ""
        ));

        mockMvc.perform(post("/api/flashflow/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100002));
    }

    @Test
    @Order(4)
    @DisplayName("不存在的用户应该返回 LOGIN_FAILED")
    void unknownUserShouldFail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "account", "nonexist",
                "password", "Admin@123"
        ));

        mockMvc.perform(post("/api/flashflow/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(110006));
    }
}
