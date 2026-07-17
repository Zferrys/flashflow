package com.flashflow.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.entity.SysRole;
import com.flashflow.auth.service.SysRoleService;
import com.flashflow.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/flashflow/auth/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "角色分页")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public R<IPage<SysRole>> page(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String keyword) {
        return R.ok(sysRoleService.page(new Page<>(page, size), keyword));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@RequestBody SysRole role) {
        sysRoleService.create(role);
        return R.ok();
    }

    @Operation(summary = "修改角色")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@RequestBody SysRole role) {
        sysRoleService.update(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return R.ok();
    }
}
