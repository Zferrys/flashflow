package com.flashflow.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * C 端用户
 */
@Data
@TableName("user_info")
public class UserInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邮箱（登录账号） */
    private String email;

    /** 手机号（选填） */
    private String phone;

    /** 密码（BCrypt） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别 0未知 1男 2女 */
    private Integer gender;

    /** 状态 1正常 0禁用 */
    private Integer status;

    /** 邀请码（预留分销） */
    private String inviteCode;

    /** 邀请人ID（预留分销） */
    private Long inviterId;

    /** 最后登录时间 */
    private LocalDateTime lastLogin;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
