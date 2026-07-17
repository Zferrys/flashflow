package com.flashflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfoBrief user;

    @Data
    @Builder
    @AllArgsConstructor
    public static class UserInfoBrief {
        private Long id;
        private String username;
        private String realName;
        private String roleCode;
    }
}
