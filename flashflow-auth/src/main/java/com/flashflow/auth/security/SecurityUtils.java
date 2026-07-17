package com.flashflow.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类：获取当前登录用户信息
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户 ID
     */
    public static Long getCurrentUserId() {
        LoginUser loginUser = getCurrentUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        LoginUser loginUser = getCurrentUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }

    /**
     * 获取当前登录用户对象
     */
    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 判断当前用户是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
