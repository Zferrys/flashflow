-- FlashFlow 优惠券表 + 演示数据
CREATE DATABASE IF NOT EXISTS flashflow_promotion DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE flashflow_promotion;

CREATE TABLE IF NOT EXISTS coupon (
  `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`            varchar(100) NOT NULL COMMENT '优惠券名称',
  `type`            tinyint(4)   NOT NULL DEFAULT '1' COMMENT '类型 1满减券 2折扣券',
  `condition_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '使用门槛',
  `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '满减金额(仅type=1)',
  `discount_rate`   decimal(3,2)  DEFAULT NULL COMMENT '折扣率(仅type=2,如0.95=95折)',
  `total_count`     int(11)      NOT NULL COMMENT '发行总量',
  `remain_count`    int(11)      NOT NULL COMMENT '剩余数量',
  `per_user_limit`  int(11)      NOT NULL DEFAULT '1' COMMENT '每人限领',
  `start_time`      datetime     NOT NULL COMMENT '开始时间',
  `end_time`        datetime     NOT NULL COMMENT '结束时间',
  `status`          tinyint(4)   NOT NULL DEFAULT '1' COMMENT '状态 1有效 0无效',
  `scope`           varchar(20)  NOT NULL DEFAULT 'ALL' COMMENT '范围:ALL=全场,CATEGORY=分类,SKU=指定商品',
  `scope_value`     varchar(500) DEFAULT NULL COMMENT '范围值:分类ID或SKU ID JSON数组',
  `auto_grant`      varchar(20)  NOT NULL DEFAULT 'NONE' COMMENT '自动发放:NONE=手动,NEW_USER=新用户,FIRST_ORDER=首单',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_time` (`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

CREATE TABLE IF NOT EXISTS user_coupon (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint(20) NOT NULL COMMENT '用户ID',
  `coupon_id`   bigint(20) NOT NULL COMMENT '优惠券ID',
  `used`        tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否已用 0未用 1已用',
  `used_time`   datetime   DEFAULT NULL COMMENT '使用时间',
  `order_sn`    varchar(32) DEFAULT NULL COMMENT '关联订单号',
  `get_time`    datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`),
  KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

-- 演示优惠券（绑定真实品类/商品）
INSERT INTO coupon (id, name, type, condition_amount, discount_amount, discount_rate, total_count, remain_count, per_user_limit, start_time, end_time, status, scope, scope_value, auto_grant) VALUES
(1, '新用户注册礼',       1, 0.00,    20.00,   NULL, 1000, 1000, 1, '2025-01-01', '2027-12-31', 1, 'ALL',      NULL,     'NEW_USER'),
(2, '618大促满减券',       1, 500.00,  66.00,   NULL, 500,  500,  1, '2026-06-01', '2026-06-30', 1, 'ALL',      NULL,     'NONE'),
(3, '全场95折券',          2, 0.00,    NULL,    0.95, 200,  200,  1, '2025-01-01', '2027-12-31', 1, 'ALL',      NULL,     'NONE'),
(4, '手机数码专享券',      1, 2000.00, 150.00,  NULL, 500,  500,  1, '2025-06-01', '2027-12-31', 1, 'CATEGORY', '1',      'NONE'),
(5, '笔记本满减券',        1, 8000.00, 500.00,  NULL, 200,  200,  1, '2025-06-01', '2027-12-31', 1, 'CATEGORY', '2',      'NONE'),
(6, '配件数码专享券',      1, 99.00,   30.00,   NULL, 300,  300,  2, '2025-01-01', '2027-12-31', 1, 'CATEGORY', '7',      'NONE'),
(7, 'AirPods 专属券',      1, 99.00,   50.00,   NULL, 100,  100,  1, '2025-01-01', '2027-12-31', 1, 'SKU',      '[7]',    'NONE'),
(8, 'iPhone 16 Pro 专属券',1, 5000.00, 300.00,  NULL, 100,  100,  1, '2025-01-01', '2027-12-31', 1, 'SKU',      '[17]',   'NONE');
