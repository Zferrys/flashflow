package com.flashflow.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.auth.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND status = 1")
    SysUser selectActiveByUsername(@Param("username") String username);
}
