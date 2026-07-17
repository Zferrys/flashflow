package com.flashflow.auth.service.impl;

import com.flashflow.auth.dao.UserInfoMapper;
import com.flashflow.auth.dto.LoginRequest;
import com.flashflow.auth.dto.LoginResponse;
import com.flashflow.auth.dto.UserRegisterRequest;
import com.flashflow.auth.entity.UserInfo;
import com.flashflow.auth.security.JwtTokenProvider;
import com.flashflow.auth.service.UserService;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * C 端用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserInfoMapper userInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedissonClient redissonClient;

    /** 登录失败锁定：5次/30分钟 */
    private static final int MAX_LOGIN_FAIL = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Override
    public void register(UserRegisterRequest request) {
        // 检查手机号是否已注册
        UserInfo exist = userInfoMapper.selectByPhone(request.getPhone());
        if (exist != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTED);
        }

        UserInfo user = new UserInfo();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getPhone().substring(7));
        user.setStatus(1);
        userInfoMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String phone = request.getUsername();

        // 1. 检查是否已被锁定（使用 RAtomicLong 原子读取）
        String lockKey = "flashflow:auth:login:fail:" + phone;
        long failCount = redissonClient.getAtomicLong(lockKey).get();
        if (failCount >= MAX_LOGIN_FAIL) {
            log.warn("账号已锁定: phone={}, failCount={}", phone, failCount);
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }

        // 2. 查询用户
        UserInfo user = userInfoMapper.selectActiveByPhone(phone);
        if (user == null) {
            incrementFailCount(lockKey);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementFailCount(lockKey);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 4. 登录成功，清除失败计数
        redissonClient.getAtomicLong(lockKey).delete();

        // 5. 更新最后登录时间
        user.setLastLogin(LocalDateTime.now());
        userInfoMapper.updateById(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getPhone(), "ROLE_USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(604800000L)
                .build();
    }

    /** 递增登录失败计数（原子操作，防止并发竞态） */
    private void incrementFailCount(String lockKey) {
        // 使用 RAtomicLong 保证原子递增
        long newCount = redissonClient.getAtomicLong(lockKey).incrementAndGet();
        redissonClient.getAtomicLong(lockKey).expire(Duration.ofMinutes(LOCK_DURATION_MINUTES));
        if (newCount >= MAX_LOGIN_FAIL) {
            log.warn("账号登录锁定: key={}, count={}", lockKey, newCount);
        }
    }

    @Override
    public long count() {
        return userInfoMapper.selectCount(null);
    }
}
