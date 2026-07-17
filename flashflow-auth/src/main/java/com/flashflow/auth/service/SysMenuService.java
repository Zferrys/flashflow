package com.flashflow.auth.service;

import com.flashflow.auth.entity.SysMenu;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface SysMenuService {

    List<SysMenu> getMenuTree();

    void create(SysMenu menu);

    void update(SysMenu menu);

    void delete(Long id);
}
