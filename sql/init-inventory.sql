-- FlashFlow Inventory 数据库初始化
USE flashflow_inventory;

-- 库存主表
CREATE TABLE IF NOT EXISTS inventory (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id`      bigint(20) NOT NULL COMMENT 'SKU_ID',
  `total_stock` int(11)    NOT NULL DEFAULT '0' COMMENT '总库存',
  `shard_count` int(11)    NOT NULL DEFAULT '16' COMMENT '分片数量',
  `version`     int(11)    NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存主表';

-- 库存分片表
CREATE TABLE IF NOT EXISTS inventory_shard (
  `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id`       bigint(20) NOT NULL COMMENT 'SKU_ID',
  `shard_index`  tinyint(4) NOT NULL COMMENT '分片索引(0~15)',
  `shard_stock`  int(11)    NOT NULL DEFAULT '0' COMMENT '分片库存',
  `frozen_stock` int(11)    NOT NULL DEFAULT '0' COMMENT '已冻结(预扣)数量',
  `create_time`  datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_shard` (`sku_id`,`shard_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存分片表';

-- 库存变动日志
CREATE TABLE IF NOT EXISTS inventory_log (
  `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_id`      bigint(20)  NOT NULL COMMENT 'SKU_ID',
  `shard_index` tinyint(4)  DEFAULT NULL COMMENT '分片索引',
  `order_id`    bigint(20)  DEFAULT NULL COMMENT '订单ID',
  `order_sn`    varchar(32) DEFAULT NULL COMMENT '订单号(冗余)',
  `quantity`    int(11)     NOT NULL COMMENT '变动数量(正为扣,负为释放)',
  `type`        varchar(20) NOT NULL COMMENT '变动类型 DEDUCT/RELEASE/CONFIRM',
  `before_stock` int(11)    NOT NULL COMMENT '变动前库存',
  `after_stock`  int(11)    NOT NULL COMMENT '变动后库存',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志(审计)';
