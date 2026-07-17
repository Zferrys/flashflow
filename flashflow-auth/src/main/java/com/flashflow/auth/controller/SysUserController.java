package com.flashflow.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.dao.UserInfoMapper;
import com.flashflow.auth.entity.UserInfo;
import com.flashflow.common.annotation.OperLog;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import com.flashflow.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * C端用户管理控制器（管理员后台管理注册用户）
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/flashflow/auth/user")
@RequiredArgsConstructor
public class SysUserController {

    private final UserInfoMapper userInfoMapper;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "用户分页")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public R<IPage<UserInfo>> page(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword) {
        Page<UserInfo> p = new Page<>(page, size);
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserInfo::getEmail, keyword)
                   .or().like(UserInfo::getNickname, keyword)
                   .or().like(UserInfo::getPhone, keyword);
        }
        wrapper.orderByDesc(UserInfo::getCreateTime);
        return R.ok(userInfoMapper.selectPage(p, wrapper));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<UserInfo> getById(@PathVariable Long id) {
        UserInfo user = userInfoMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(null); // 不返回密码
        return R.ok(user);
    }

    @OperLog(module = "用户管理", operation = "新增用户")
    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@RequestBody UserInfo user) {
        // 检查邮箱唯一
        if (StringUtils.hasText(user.getEmail())) {
            UserInfo exist = userInfoMapper.selectByEmail(user.getEmail());
            if (exist != null) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTED);
            }
        }
        // 密码加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userInfoMapper.insert(user);
        return R.ok();
    }

    @OperLog(module = "用户管理", operation = "修改用户")
    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@RequestBody UserInfo user) {
        UserInfo exist = userInfoMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 密码不为空时更新密码
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }
        userInfoMapper.updateById(user);
        return R.ok();
    }

    @OperLog(module = "用户管理", operation = "删除用户")
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        UserInfo exist = userInfoMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userInfoMapper.deleteById(id);
        return R.ok();
    }
}
