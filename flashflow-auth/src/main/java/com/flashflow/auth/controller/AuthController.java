package com.flashflow.auth.controller;

import com.flashflow.auth.dto.LoginRequest;
import com.flashflow.auth.dto.LoginResponse;
import com.flashflow.auth.dto.UserRegisterRequest;
import com.flashflow.auth.security.JwtTokenProvider;
import com.flashflow.auth.security.LoginUser;
import com.flashflow.auth.security.SecurityUtils;
import com.flashflow.auth.service.MailService;
import com.flashflow.auth.service.UserService;
import com.flashflow.common.annotation.OperLog;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import com.flashflow.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * 认证控制器（登录/注册/登出）
 */
@RestController
@RequestMapping("/api/flashflow/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final MailService mailService;
    private final RedissonClient redissonClient;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.generateAccessToken(
                    loginUser.getId(), loginUser.getUsername(), loginUser.getRoleCode());
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginUser.getId());

            LoginResponse.UserInfoBrief userInfo = LoginResponse.UserInfoBrief.builder()
                    .id(loginUser.getId())
                    .username(loginUser.getUsername())
                    .roleCode(loginUser.getRoleCode())
                    .build();

            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(604800000L)
                    .user(userInfo)
                    .build();

            return R.ok(response);
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
    }

    /**
     * 管理员/用户登出（JWT 加入 Redis 黑名单，使 Token 立即失效）
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);
            long ttl = Math.max(0, expiration.getTime() - System.currentTimeMillis());
            if (ttl > 0) {
                String jti = jwtTokenProvider.getUserIdFromToken(token).toString()
                        + "_" + System.currentTimeMillis();
                redissonClient.getBucket("flashflow:auth:jwt:blacklist:" + jti)
                        .set("1", Duration.ofMillis(ttl));
            }
        }
        return R.ok();
    }

    /**
     * Token 刷新（用 RefreshToken 换取新 AccessToken）
     * 使用 @RequestBody 防止 refreshToken 出现在 URL/日志中
     */
    @PostMapping("/refresh")
    public R<LoginResponse> refreshToken(@RequestBody java.util.Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 安全校验：必须是 RefreshToken（不能拿 AccessToken 来刷新）
        if (!"refresh".equals(jwtTokenProvider.getClaimFromToken(refreshToken, "type"))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String role = jwtTokenProvider.getClaimFromToken(refreshToken, "role");
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username, role != null ? role : "ROLE_USER");

        LoginResponse response = LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(604800000L)
                .build();
        return R.ok(response);
    }

    /**
     * C 端用户注册
     */
    @PostMapping("/user/register")
    public R<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userService.register(request);
        return R.ok();
    }

    /**
     * C 端用户登录
     */
    @PostMapping("/user/login")
    public R<LoginResponse> userLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return R.ok(response);
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/user/send-code")
    public R<Void> sendCode(@RequestParam String email) {
        mailService.sendVerifyCode(email);
        return R.ok();
    }

    /**
     * 校验邮箱验证码
     */
    @PostMapping("/user/verify-code")
    public R<Boolean> verifyCode(@RequestParam String email, @RequestParam String code) {
        return R.ok(mailService.verifyCode(email, code, 0));
    }

    /**
     * C 端用户统计总数
     */
    @GetMapping("/user/count")
    public R<Long> userCount() {
        return R.ok(userService.count());
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public R<LoginUser> me() {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return R.ok(user);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
