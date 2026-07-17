package com.flashflow.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单权限
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 权限标识（sys:user:list） */
    private String permission;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 类型 0目录 1菜单 2按钮 */
    private Integer type;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 状态 1显示 0隐藏 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 子菜单（非数据库字段） */
    @TableField(exist = false)
    private List<SysMenu> children;
}
