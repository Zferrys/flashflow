-- FlashFlow Promotion 数据库初始化
USE flashflow_promotion;

-- 营销活动
CREATE TABLE IF NOT EXISTS promotion_activity (
  `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `activity_type` varchar(20) NOT NULL COMMENT '活动类型 FLASH_SALE/PRE_SALE/GROUP_BUY',
  `name`          varchar(100) NOT NULL COMMENT '活动名称',
  `start_time`    datetime    NOT NULL COMMENT '开始时间',
  `end_time`      datetime    NOT NULL COMMENT '结束时间',
  `status`        tinyint(4)  NOT NULL DEFAULT '0' COMMENT '状态 0草稿 1待预热 2进行中 3已结束 4已关闭',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by`     bigint(20)  DEFAULT NULL COMMENT '创建人',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime    DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`activity_type`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`)
  KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动';

-- 活动商品
CREATE TABLE IF NOT EXISTS promotion_sku (
  `id`              bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `activity_id`     bigint(20)  NOT NULL COMMENT '活动ID',
  `sku_id`          bigint(20)  NOT NULL COMMENT '商品SKU_ID',
  `sku_name`        varchar(200) NOT NULL COMMENT '商品名称(冗余)',
  `sku_image`       varchar(500) DEFAULT NULL COMMENT '商品图片(冗余)',
  `original_price`  decimal(10,2) NOT NULL COMMENT '原价',
  `activity_price`  decimal(10,2) NOT NULL COMMENT '活动价',
  `stock_limit`     int(11)     NOT NULL DEFAULT '0' COMMENT '活动库存上限',
  `per_user_limit`  int(11)     NOT NULL DEFAULT '1' COMMENT '每人限购数量',
  `sold_count`      int(11)     NOT NULL DEFAULT '0' COMMENT '已售数量',
  `sort`            int(11)     NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_sku` (`activity_id`,`sku_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与商品';

-- 活动参与记录
CREATE TABLE IF NOT EXISTS promotion_record (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `activity_id` bigint(20) NOT NULL COMMENT '活动ID',
  `sku_id`      bigint(20) NOT NULL COMMENT 'SKU_ID',
  `user_id`     bigint(20) NOT NULL COMMENT '用户ID',
  `order_id`    bigint(20) DEFAULT NULL COMMENT '关联订单ID',
  `quantity`    int(11)    NOT NULL DEFAULT '1' COMMENT '购买数量',
  `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity` (`user_id`,`activity_id`,`sku_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与记录(防重复)';
