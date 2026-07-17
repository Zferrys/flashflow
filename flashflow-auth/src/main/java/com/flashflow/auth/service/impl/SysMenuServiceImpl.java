package com.flashflow.auth.service.impl;

import com.flashflow.auth.dao.SysMenuMapper;
import com.flashflow.auth.entity.SysMenu;
import com.flashflow.auth.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> allMenus = sysMenuMapper.selectAllActive();
        return buildTree(allMenus, 0L);
    }

    @Override
    public void create(SysMenu menu) {
        sysMenuMapper.insert(menu);
    }

    @Override
    public void update(SysMenu menu) {
        sysMenuMapper.updateById(menu);
    }

    @Override
    public void delete(Long id) {
        sysMenuMapper.deleteById(id);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId().equals(parentId)) {
                menu.setChildren(buildTree(menus, menu.getId()));
                tree.add(menu);
            }
        }
        return tree;
    }
}
