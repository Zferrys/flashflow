package com.flashflow.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.auth.entity.UserInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * C 端用户 Mapper
 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    @Select("SELECT * FROM user_info WHERE phone = #{phone}")
    UserInfo selectByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM user_info WHERE phone = #{phone} AND status = 1")
    UserInfo selectActiveByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM user_info WHERE email = #{email}")
    UserInfo selectByEmail(@Param("email") String email);

    @Select("SELECT * FROM user_info WHERE email = #{email} AND status = 1")
    UserInfo selectActiveByEmail(@Param("email") String email);
}
