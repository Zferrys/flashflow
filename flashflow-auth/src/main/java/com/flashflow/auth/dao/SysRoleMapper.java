package com.flashflow.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.auth.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 根据用户ID查询角色列表 */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1")
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
