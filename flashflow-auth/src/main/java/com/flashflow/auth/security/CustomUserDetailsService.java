package com.flashflow.auth.security;

import com.flashflow.auth.dao.SysMenuMapper;
import com.flashflow.auth.dao.SysRoleMapper;
import com.flashflow.auth.dao.SysUserMapper;
import com.flashflow.auth.entity.SysMenu;
import com.flashflow.auth.entity.SysRole;
import com.flashflow.auth.entity.SysUser;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 用户加载服务（从数据库加载真实 RBAC 角色权限）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return buildLoginUser(user);
    }

    /**
     * 根据用户 ID 加载（JWT 过滤器使用）
     */
    public UserDetails loadUserById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在, id: " + userId);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        // 从数据库加载用户角色
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        String primaryRoleCode = "ROLE_USER";
        if (!roles.isEmpty()) {
            // 角色作为权限（Spring Security hasRole 检查需要 ROLE_ 前缀）
            authorities.addAll(roles.stream()
                    .map(r -> new SimpleGrantedAuthority(r.getCode()))
                    .collect(Collectors.toSet()));
            primaryRoleCode = roles.get(0).getCode();

            // 加载每个角色的菜单权限
            for (SysRole role : roles) {
                List<SysMenu> menus = sysMenuMapper.selectByRoleId(role.getId());
                for (SysMenu menu : menus) {
                    if (menu.getPermission() != null && !menu.getPermission().isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority(menu.getPermission()));
                    }
                }
            }
        }

        // 确保至少有一个基本角色
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        log.debug("用户 {} 加载权限: {}", user.getUsername(), authorities);
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                primaryRoleCode,
                authorities,
                user.getStatus() == 1
        );
    }
}
