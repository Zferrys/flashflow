package com.flashflow.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.auth.entity.SysMenu;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT * FROM sys_menu WHERE status = 1 ORDER BY sort ASC")
    List<SysMenu> selectAllActive();

    @Select("SELECT * FROM sys_menu WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort ASC")
    List<SysMenu> selectByParentId(Long parentId);

    /** 根据角色ID查询菜单权限 */
    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.status = 1 ORDER BY m.sort ASC")
    List<SysMenu> selectByRoleId(Long roleId);
}
