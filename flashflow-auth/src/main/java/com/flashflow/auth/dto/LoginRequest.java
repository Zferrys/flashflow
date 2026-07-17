package com.flashflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（C端用户用邮箱登录，管理员用用户名登录）
 */
@Data
public class LoginRequest {

    @NotBlank(message = "邮箱/用户名不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
