package com.flashflow.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.entity.UserInfo;
import com.flashflow.auth.service.UserService;
import com.flashflow.common.annotation.OperLog;
import com.flashflow.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * C端用户管理控制器（管理员后台管理注册用户）
 * 业务逻辑统一委托给 UserService，Controller 只做参数转发
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/flashflow/auth/user")
@RequiredArgsConstructor
public class SysUserController {

    private final UserService userService;

    @Operation(summary = "用户分页")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public R<IPage<UserInfo>> page(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword) {
        return R.ok(userService.pageUsers(new Page<>(page, size), keyword));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<UserInfo> getById(@PathVariable Long id) {
        UserInfo user = userService.getById(id);
        user.setPassword(null); // 不返回密码
        return R.ok(user);
    }

    @OperLog(module = "用户管理", operation = "新增用户")
    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@RequestBody UserInfo user) {
        userService.createUser(user);
        return R.ok();
    }

    @OperLog(module = "用户管理", operation = "修改用户")
    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@RequestBody UserInfo user) {
        userService.updateUser(user);
        return R.ok();
    }

    @OperLog(module = "用户管理", operation = "删除用户")
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}
