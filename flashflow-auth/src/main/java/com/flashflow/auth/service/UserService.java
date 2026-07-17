package com.flashflow.auth.service;

import com.flashflow.auth.dto.LoginRequest;
import com.flashflow.auth.dto.LoginResponse;
import com.flashflow.auth.dto.UserRegisterRequest;
import com.flashflow.auth.entity.UserInfo;

/**
 * C 端用户服务接口
 */
public interface UserService {

    /** 注册 */
    void register(UserRegisterRequest request);

    /** 登录 */
    LoginResponse login(LoginRequest request);

    /** 获取 C 端用户总数 */
    long count();
}
