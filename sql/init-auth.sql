-- FlashFlow Auth 数据库初始化
USE flashflow_auth;

-- 系统用户（管理员）
CREATE TABLE IF NOT EXISTS sys_user (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    varchar(50)  NOT NULL COMMENT '用户名',
  `password`    varchar(200) NOT NULL COMMENT '密码(BCrypt)',
  `salt`        varchar(50)  DEFAULT NULL COMMENT '盐(兼容旧系统)',
  `email`       varchar(100) DEFAULT NULL COMMENT '邮箱',
  `mobile`      varchar(20)  DEFAULT NULL COMMENT '手机号',
  `real_name`   varchar(50)  DEFAULT NULL COMMENT '真实姓名',
  `status`      tinyint(4)   NOT NULL DEFAULT '1' COMMENT '状态 1正常 0禁用',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户（管理员）';

-- 角色
CREATE TABLE IF NOT EXISTS sys_role (
  `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        varchar(50) NOT NULL COMMENT '角色名称',
  `code`        varchar(50) NOT NULL COMMENT '角色编码(ROLE_ADMIN)',
  `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
  `status`      tinyint(4)  NOT NULL DEFAULT '1' COMMENT '状态 1正常 0禁用',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- 菜单权限
CREATE TABLE IF NOT EXISTS sys_menu (
  `id`         bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`  bigint(20)  NOT NULL DEFAULT '0' COMMENT '父菜单ID',
  `name`       varchar(50) NOT NULL COMMENT '菜单名称',
  `permission` varchar(200) DEFAULT NULL COMMENT '权限标识(sys:user:list)',
  `path`       varchar(200) DEFAULT NULL COMMENT '路由路径',
  `component`  varchar(200) DEFAULT NULL COMMENT '组件路径',
  `type`       tinyint(4)  NOT NULL DEFAULT '0' COMMENT '类型 0目录 1菜单 2按钮',
  `icon`       varchar(50)  DEFAULT NULL COMMENT '图标',
  `sort`       int(11)     NOT NULL DEFAULT '0' COMMENT '排序',
  `status`     tinyint(4)  NOT NULL DEFAULT '1' COMMENT '状态 1显示 0隐藏',
  `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限';

-- 用户角色关联
CREATE TABLE IF NOT EXISTS sys_user_role (
  `id`       bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`  bigint(20) NOT NULL COMMENT '用户ID',
  `role_id`  bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- 角色菜单关联
CREATE TABLE IF NOT EXISTS sys_role_menu (
  `id`      bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- 操作日志
CREATE TABLE IF NOT EXISTS sys_oper_log (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint(20)   DEFAULT NULL COMMENT '操作人ID',
  `username`    varchar(50)  DEFAULT NULL COMMENT '操作人用户名',
  `module`      varchar(50)  NOT NULL COMMENT '操作模块',
  `operation`   varchar(100) NOT NULL COMMENT '操作类型(新增/修改/删除)',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `method`      varchar(10)  DEFAULT NULL COMMENT '请求方式',
  `params`      text         COMMENT '请求参数',
  `result`      tinyint(4)   NOT NULL DEFAULT '1' COMMENT '结果 1成功 0失败',
  `error_msg`   text         COMMENT '错误信息',
  `cost_time`   int(11)      DEFAULT NULL COMMENT '耗时(ms)',
  `ip`          varchar(50)  DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志';

-- C 端用户
CREATE TABLE IF NOT EXISTS user_info (
  `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone`       varchar(20) NOT NULL COMMENT '手机号(登录账号)',
  `password`    varchar(200) NOT NULL COMMENT '密码(BCrypt)',
  `nickname`    varchar(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`      varchar(500) DEFAULT NULL COMMENT '头像URL',
  `gender`      tinyint(4)   DEFAULT '0' COMMENT '性别 0未知 1男 2女',
  `status`      tinyint(4)   NOT NULL DEFAULT '1' COMMENT '状态 1正常 0禁用',
  `invite_code` varchar(20)  DEFAULT NULL COMMENT '邀请码(预留分销)',
  `inviter_id`  bigint(20)   DEFAULT NULL COMMENT '邀请人ID(预留分销)',
  `last_login`  datetime     DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_inviter` (`inviter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端用户';

-- C端用户收货地址
CREATE TABLE IF NOT EXISTS user_address (
  `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint(20)  NOT NULL COMMENT '用户ID',
  `name`        varchar(50) NOT NULL COMMENT '收货人姓名',
  `phone`       varchar(20) NOT NULL COMMENT '收货人手机号',
  `province`    varchar(20) NOT NULL COMMENT '省',
  `city`        varchar(20) NOT NULL COMMENT '市',
  `district`    varchar(20) NOT NULL COMMENT '区',
  `detail`      varchar(200) NOT NULL COMMENT '详细地址',
  `zip_code`    varchar(10)  DEFAULT NULL COMMENT '邮编',
  `is_default`  tinyint(4)   NOT NULL DEFAULT '0' COMMENT '是否默认 1是 0否',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址';

-- 邮箱验证码
CREATE TABLE IF NOT EXISTS email_verify (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`       varchar(100) NOT NULL COMMENT '邮箱',
  `code`        varchar(10)  NOT NULL COMMENT '验证码',
  `type`        tinyint(4)   NOT NULL DEFAULT '1' COMMENT '类型 1注册 2找回密码',
  `status`      tinyint(4)   NOT NULL DEFAULT '0' COMMENT '状态 0未使用 1已使用',
  `expire_time` datetime     NOT NULL COMMENT '过期时间',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_email_type` (`email`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码';

-- 初始化管理员账号
INSERT INTO sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 1);

-- 初始化角色
INSERT INTO sys_role (name, code, remark) VALUES
('超级管理员', 'ROLE_ADMIN', '系统超级管理员'),
('普通用户', 'ROLE_USER', '普通用户');

-- 分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
