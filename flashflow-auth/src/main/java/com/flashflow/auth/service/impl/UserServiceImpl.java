package com.flashflow.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.dao.UserInfoMapper;
import com.flashflow.auth.dto.LoginRequest;
import com.flashflow.auth.dto.LoginResponse;
import com.flashflow.auth.dto.UserRegisterRequest;
import com.flashflow.auth.entity.UserInfo;
import com.flashflow.auth.security.JwtTokenProvider;
import com.flashflow.auth.service.MailService;
import com.flashflow.auth.service.UserService;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

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
    private final MailService mailService;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${promotion.service.url:http://127.0.0.1:8100/api/flashflow/promotion}")
    private String promotionBaseUrl;

    /** 登录失败锁定：5次/30分钟 */
    private static final int MAX_LOGIN_FAIL = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Override
    public void register(UserRegisterRequest request) {
        // 1. 验证邮箱验证码（必须在注册前校验）
        if (!mailService.verifyCode(request.getEmail(), request.getVerifyCode(), 0)) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }

        // 2. 检查邮箱是否已注册
        UserInfo exist = userInfoMapper.selectByEmail(request.getEmail());
        if (exist != null) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTED);
        }

        // 3. 创建用户
        UserInfo user = new UserInfo();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone()); // 手机号选填
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getEmail().split("@")[0]);
        user.setStatus(1);
        userInfoMapper.insert(user);

        // 4. 自动发放新用户优惠券（异步调用，失败不影响注册）
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", String.valueOf(user.getId()));
            restTemplate.postForObject(
                    promotionBaseUrl + "/coupon/auto-grant?grantType=NEW_USER",
                    new HttpEntity<>(null, headers), String.class);
            log.info("新用户优惠券已发放: userId={}", user.getId());
        } catch (Exception e) {
            log.warn("新用户优惠券发放失败（优惠券服务可能未启动）: userId={}", user.getId(), e);
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.getAccount();

        // 1. 检查是否已被锁定（使用 RAtomicLong 原子读取）
        String lockKey = "flashflow:auth:login:fail:" + email;
        long failCount = redissonClient.getAtomicLong(lockKey).get();
        if (failCount >= MAX_LOGIN_FAIL) {
            log.warn("账号已锁定: email={}, failCount={}", email, failCount);
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }

        // 2. 用邮箱查询用户
        UserInfo user = userInfoMapper.selectActiveByEmail(email);
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

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), "ROLE_USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), "ROLE_USER");

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
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

    @Override
    public UserInfo getById(Long id) {
        UserInfo user = userInfoMapper.selectById(id);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return user;
    }

    @Override
    public void updateProfile(Long userId, String nickname, String phone) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        userInfoMapper.updateById(user);
        log.info("用户信息已更新: userId={}", userId);
    }

    // ========== 管理员用户管理 ==========

    @Override
    public IPage<UserInfo> pageUsers(Page<UserInfo> page, String keyword) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserInfo::getEmail, keyword)
                   .or().like(UserInfo::getNickname, keyword)
                   .or().like(UserInfo::getPhone, keyword);
        }
        wrapper.orderByDesc(UserInfo::getCreateTime);
        return userInfoMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserInfo user) {
        if (StringUtils.hasText(user.getEmail())) {
            UserInfo exist = userInfoMapper.selectByEmail(user.getEmail());
            if (exist != null) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTED);
            }
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userInfoMapper.insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserInfo user) {
        UserInfo exist = userInfoMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }
        userInfoMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        UserInfo exist = userInfoMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userInfoMapper.deleteById(id);
    }
}
