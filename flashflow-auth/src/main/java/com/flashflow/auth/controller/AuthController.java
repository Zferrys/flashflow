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
import org.redisson.api.RAtomicLong;
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
     * 管理员登录（含 IP 限流：60秒内最多 5 次）
     */
    @OperLog(module = "认证", operation = "管理员登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        rateLimitIP(httpRequest, "admin-login", 20, 60);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getAccount(), request.getPassword())
            );

            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.generateAccessToken(
                    loginUser.getId(), loginUser.getUsername(), loginUser.getRoleCode());
            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    loginUser.getId(), loginUser.getUsername(), loginUser.getRoleCode());

            LoginResponse.UserInfoBrief userInfo = LoginResponse.UserInfoBrief.builder()
                    .id(loginUser.getId())
                    .username(loginUser.getUsername())
                    .roleCode(loginUser.getRoleCode())
                    .build();

            long expiresIn = jwtTokenProvider.getAccessTokenExpiration();
            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
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
    @OperLog(module = "认证", operation = "管理员登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);
            long ttl = Math.max(0, expiration.getTime() - System.currentTimeMillis());
            if (ttl > 0) {
                String jti = jwtTokenProvider.getJtiFromToken(token);
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
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
        return R.ok(response);
    }

    /**
     * C 端用户注册
     */
    @PostMapping("/user/register")
    public R<Void> register(@Valid @RequestBody UserRegisterRequest request,
                             HttpServletRequest httpRequest) {
        // IP 级限流：60秒内最多 3 次注册（防批量注册攻击）
        rateLimitIP(httpRequest, "register", 3, 60);
        userService.register(request);
        return R.ok();
    }

    /**
     * C 端用户登录
     */
    @PostMapping("/user/login")
    public R<LoginResponse> userLogin(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        // IP 级限流：60秒内最多 10 次（补充账号级锁定的缺口）
        rateLimitIP(httpRequest, "login", 10, 60);
        LoginResponse response = userService.login(request);
        return R.ok(response);
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/user/send-code")
    public R<Void> sendCode(@RequestParam String email, HttpServletRequest httpRequest) {
        // 双重限流：同邮箱 60秒只能发 1 次 + 同 IP 60秒最多 5 次
        rateLimitByKey("flashflow:ratelimit:sendcode:email:" + email, 1, 60, "验证码发送过于频繁，请60秒后再试");
        rateLimitIP(httpRequest, "sendcode", 5, 60);
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
    public R<java.util.Map<String, Object>> me() {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // C端用户从 user_info 加载完整信息
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("roleCode", user.getRoleCode());
        // 尝试加载 C端用户详细信息
        com.flashflow.auth.entity.UserInfo userInfo = userService.getById(user.getId());
        if (userInfo != null) {
            result.put("nickname", userInfo.getNickname());
            result.put("email", userInfo.getEmail());
            result.put("phone", userInfo.getPhone());
            result.put("avatar", userInfo.getAvatar());
        }
        return R.ok(result);
    }

    /**
     * 修改当前用户个人信息（昵称/手机号）
     */
    @PutMapping("/me")
    public R<Void> updateProfile(@RequestBody java.util.Map<String, String> body) {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String nickname = body.get("nickname");
        String phone = body.get("phone");
        userService.updateProfile(user.getId(), nickname, phone);
        return R.ok();
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /** 获取客户端真实 IP（考虑反向代理） */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /** IP 级 Redis 限流 */
    private void rateLimitIP(HttpServletRequest request, String action, int maxRequests, int windowSeconds) {
        String ip = getClientIP(request);
        String key = "flashflow:ratelimit:" + action + ":" + ip;
        rateLimitByKey(key, maxRequests, windowSeconds, "操作过于频繁，请" + windowSeconds + "秒后再试");
    }

    /** 通用 Redis 计数器限流 */
    private void rateLimitByKey(String key, int maxRequests, int windowSeconds, String errorMsg) {
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long count = counter.incrementAndGet();
        if (count == 1) {
            counter.expire(Duration.ofSeconds(windowSeconds));
        }
        if (count > maxRequests) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, errorMsg);
        }
    }
}
