package com.flashflow.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.entity.SysRole;

/**
 * 角色服务接口
 */
public interface SysRoleService {

    IPage<SysRole> page(Page<SysRole> page, String keyword);

    void create(SysRole role);

    void update(SysRole role);

    void delete(Long id);
}
