package com.flashflow.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.auth.dao.SysUserMapper;
import com.flashflow.auth.entity.SysUser;
import com.flashflow.auth.service.SysUserService;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 系统用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public IPage<SysUser> page(Page<SysUser> page, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                   .or().like(SysUser::getRealName, keyword)
                   .or().like(SysUser::getMobile, keyword);
        }
        return sysUserMapper.selectPage(page, wrapper);
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(null); // 不返回密码
        return user;
    }

    @Override
    public void create(SysUser user) {
        // 检查用户名唯一
        SysUser exist = sysUserMapper.selectByUsername(user.getUsername());
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        sysUserMapper.insert(user);
    }

    @Override
    public void update(SysUser user) {
        SysUser exist = sysUserMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 密码不为空时更新密码
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }
        sysUserMapper.updateById(user);
    }

    @Override
    public void delete(Long id) {
        SysUser exist = sysUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        sysUserMapper.deleteById(id);
    }
}
