package com.flashflow.common.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 每次请求从 Gateway 注入的 X-User-Id / X-User-Name / X-User-Role 头中提取用户身份，
 * 存入 UserContext，请求结束后自动清理。
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader("X-User-Id");
        String userName = request.getHeader("X-User-Name");
        String role = request.getHeader("X-User-Role");

        if (userIdStr != null) {
            try {
                UserContext.set(Long.parseLong(userIdStr), userName, role);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", userIdStr);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
