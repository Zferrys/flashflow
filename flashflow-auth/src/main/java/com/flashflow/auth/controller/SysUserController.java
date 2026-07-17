package com.flashflow.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.entity.SysUser;
import com.flashflow.auth.service.SysUserService;
import com.flashflow.common.annotation.OperLog;
import com.flashflow.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/flashflow/auth/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户分页")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public R<IPage<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String keyword) {
        Page<SysUser> p = new Page<>(page, size);
        return R.ok(sysUserService.page(p, keyword));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysUser> getById(@PathVariable Long id) {
        return R.ok(sysUserService.getById(id));
    }

    @OperLog(module = "用户管理", operation = "新增用户")
    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@RequestBody SysUser user) {
        sysUserService.create(user);
        return R.ok();
    }

    @OperLog(module = "用户管理", operation = "修改用户")
    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@RequestBody SysUser user) {
        sysUserService.update(user);
        return R.ok();
    }

    @OperLog(module = "用户管理", operation = "删除用户")
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return R.ok();
    }
}
