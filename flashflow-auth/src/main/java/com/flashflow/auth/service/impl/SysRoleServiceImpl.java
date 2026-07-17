package com.flashflow.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.dao.SysRoleMapper;
import com.flashflow.auth.entity.SysRole;
import com.flashflow.auth.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    public IPage<SysRole> page(Page<SysRole> page, String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysRole::getName, keyword)
                   .or().like(SysRole::getCode, keyword);
        }
        return sysRoleMapper.selectPage(page, wrapper);
    }

    @Override
    public void create(SysRole role) {
        sysRoleMapper.insert(role);
    }

    @Override
    public void update(SysRole role) {
        sysRoleMapper.updateById(role);
    }

    @Override
    public void delete(Long id) {
        sysRoleMapper.deleteById(id);
    }
}
