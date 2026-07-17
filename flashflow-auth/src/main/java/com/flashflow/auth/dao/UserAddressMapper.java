package com.flashflow.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.auth.entity.UserAddress;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserAddressMapper extends BaseMapper<UserAddress> {

    @Select("SELECT * FROM user_address WHERE user_id = #{userId} ORDER BY is_default DESC, create_time DESC")
    List<UserAddress> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_address WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    UserAddress selectDefault(@Param("userId") Long userId);

    @Update("UPDATE user_address SET is_default = 0 WHERE user_id = #{userId}")
    int clearDefault(@Param("userId") Long userId);
}
