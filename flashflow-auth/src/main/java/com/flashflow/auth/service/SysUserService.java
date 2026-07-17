package com.flashflow.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.entity.SysUser;

/**
 * 系统用户服务接口
 */
public interface SysUserService {

    IPage<SysUser> page(Page<SysUser> page, String keyword);

    SysUser getById(Long id);

    void create(SysUser user);

    void update(SysUser user);

    void delete(Long id);
}
