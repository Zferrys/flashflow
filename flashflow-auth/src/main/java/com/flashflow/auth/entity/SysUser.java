package com.flashflow.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户（管理员）
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 哈希，默认不查询（防止 SELECT * 暴露） */
    @TableField(select = false)
    private String password;

    /** BCrypt 自带盐值，此字段仅兼容旧数据，不再使用 */
    @TableField(select = false)
    private String salt;

    private String email;

    private String mobile;

    private String realName;

    /** 状态 1正常 0禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
