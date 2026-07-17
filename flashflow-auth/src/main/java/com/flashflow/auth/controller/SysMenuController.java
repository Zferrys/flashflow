package com.flashflow.auth.controller;

import com.flashflow.auth.entity.SysMenu;
import com.flashflow.auth.service.SysMenuService;
import com.flashflow.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/flashflow/auth/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @Operation(summary = "菜单树（完整）")
    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<SysMenu>> tree() {
        return R.ok(sysMenuService.getMenuTree());
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@RequestBody SysMenu menu) {
        sysMenuService.create(menu);
        return R.ok();
    }

    @Operation(summary = "修改菜单")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@RequestBody SysMenu menu) {
        sysMenuService.update(menu);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        sysMenuService.delete(id);
        return R.ok();
    }
}
