-- FlashFlow Order 数据库初始化
USE flashflow_order;

-- 订单主表
CREATE TABLE IF NOT EXISTS order_info (
  `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_sn`        varchar(32)  NOT NULL COMMENT '订单号(业务唯一)',
  `user_id`         bigint(20)   NOT NULL COMMENT '用户ID',
  `total_amount`    decimal(10,2) NOT NULL COMMENT '订单总金额',
  `pay_amount`      decimal(10,2) NOT NULL COMMENT '实付金额',
  `pay_type`        tinyint(4)   DEFAULT NULL COMMENT '支付方式 1支付宝 2微信',
  `status`          tinyint(4)   NOT NULL DEFAULT '0' COMMENT '状态 0待支付 1已支付 2已发货 3已收货 4已完成 5已取消 6退款中 7已退款',
  `coupon_id`       bigint(20)   DEFAULT NULL COMMENT '优惠券ID(预留)',
  `discount_amount` decimal(10,2) DEFAULT '0.00' COMMENT '优惠金额(预留)',
  `address_snapshot` text COMMENT '收货地址快照(JSON)',
  `payment_time`    datetime     DEFAULT NULL COMMENT '支付时间',
  `cancel_time`     datetime     DEFAULT NULL COMMENT '取消时间',
  `cancel_reason`   varchar(200) DEFAULT NULL COMMENT '取消原因',
  `finish_time`     datetime     DEFAULT NULL COMMENT '完成时间',
  `remark`          varchar(500) DEFAULT NULL COMMENT '买家备注',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      tinyint(4)   NOT NULL DEFAULT '0' COMMENT '逻辑删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sn` (`order_sn`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单明细
CREATE TABLE IF NOT EXISTS order_item (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    bigint(20)   NOT NULL COMMENT '订单ID',
  `order_sn`    varchar(32)  NOT NULL COMMENT '订单号(冗余)',
  `sku_id`      bigint(20)   NOT NULL COMMENT 'SKU_ID',
  `sku_name`    varchar(200) NOT NULL COMMENT '商品名称',
  `sku_image`   varchar(500) DEFAULT NULL COMMENT '商品图片',
  `sku_price`   decimal(10,2) NOT NULL COMMENT '商品价格',
  `quantity`    int(11)      NOT NULL DEFAULT '1' COMMENT '购买数量',
  `sub_total`   decimal(10,2) NOT NULL COMMENT '小计金额',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_sn` (`order_sn`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- 订单事件流水
CREATE TABLE IF NOT EXISTS order_event (
  `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`      bigint(20)  NOT NULL COMMENT '订单ID',
  `order_sn`      varchar(32) NOT NULL COMMENT '订单号(冗余)',
  `from_status`   tinyint(4)  DEFAULT NULL COMMENT '原状态',
  `to_status`     tinyint(4)  NOT NULL COMMENT '目标状态',
  `operator`      bigint(20)  DEFAULT NULL COMMENT '操作人ID',
  `operator_type` tinyint(4)  NOT NULL DEFAULT '0' COMMENT '操作人类型 0系统 1用户 2管理员',
  `event_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `extra_data`    json        DEFAULT NULL COMMENT '扩展数据(JSON)',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_event_time` (`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单事件流水(Event Sourcing)';

-- 本地事务表
CREATE TABLE IF NOT EXISTS local_transaction (
  `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_id`    varchar(64) NOT NULL COMMENT '消息ID(全局唯一)',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型(order.created/payment.success)',
  `business_key`  varchar(100) NOT NULL COMMENT '业务主键(order_sn/payment_id)',
  `status`        tinyint(4)  NOT NULL DEFAULT '0' COMMENT '状态 0INIT 1DONE 2FAIL 3COMPENSATED',
  `payload`       json        DEFAULT NULL COMMENT '消息体(JSON)',
  `retry_count`   int(11)     NOT NULL DEFAULT '0' COMMENT '重试次数',
  `max_retry`     int(11)     NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  `last_retry`    datetime    DEFAULT NULL COMMENT '最后重试时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime    DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_status` (`status`),
  KEY `idx_business` (`business_type`,`business_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地事务表(Saga 可靠性保证)';
