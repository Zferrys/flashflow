package com.flashflow.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.dto.LoginRequest;
import com.flashflow.auth.dto.LoginResponse;
import com.flashflow.auth.dto.UserRegisterRequest;
import com.flashflow.auth.entity.UserInfo;

/**
 * C 端用户服务接口（含管理员用户管理方法）
 */
public interface UserService {

    /** 注册 */
    void register(UserRegisterRequest request);

    /** 登录 */
    LoginResponse login(LoginRequest request);

    /** 获取 C 端用户总数 */
    long count();

    /** 根据 ID 获取用户信息 */
    UserInfo getById(Long id);

    /** 修改个人信息（昵称/手机号） */
    void updateProfile(Long userId, String nickname, String phone);

    // ========== 管理员用户管理 ==========

    /** 用户分页 */
    IPage<UserInfo> pageUsers(Page<UserInfo> page, String keyword);

    /** 创建用户（管理员后台） */
    void createUser(UserInfo user);

    /** 修改用户（管理员后台） */
    void updateUser(UserInfo user);

    /** 删除用户 */
    void deleteUser(Long id);
}
