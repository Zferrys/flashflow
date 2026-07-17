-- FlashFlow Payment 数据库初始化
USE flashflow_payment;

-- 支付订单
CREATE TABLE IF NOT EXISTS payment_order (
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_sn`      varchar(32)  NOT NULL COMMENT '订单号',
  `trade_no`      varchar(64)  DEFAULT NULL COMMENT '支付宝交易号',
  `pay_amount`    decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_type`      tinyint(4)   NOT NULL DEFAULT '1' COMMENT '支付方式 1支付宝 2微信(预留)',
  `status`        tinyint(4)   NOT NULL DEFAULT '0' COMMENT '状态 0待支付 1支付成功 2支付失败 3退款中 4已退款',
  `notify_status` tinyint(4)   NOT NULL DEFAULT '0' COMMENT '回调状态 0未收到 1已收到 2验签失败',
  `notify_data`   json         DEFAULT NULL COMMENT '回调原始数据',
  `notify_time`   datetime     DEFAULT NULL COMMENT '回调时间',
  `expire_time`   datetime     NOT NULL COMMENT '支付过期时间(30分钟)',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sn` (`order_sn`),
  KEY `idx_trade_no` (`trade_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单';

-- 退款记录
CREATE TABLE IF NOT EXISTS refund_record (
  `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_id`      bigint(20)   NOT NULL COMMENT '支付订单ID',
  `order_sn`        varchar(32)  NOT NULL COMMENT '订单号(冗余)',
  `refund_amount`   decimal(10,2) NOT NULL COMMENT '退款金额',
  `refund_reason`   varchar(500) NOT NULL COMMENT '退款原因',
  `refund_status`   tinyint(4)   NOT NULL DEFAULT '0' COMMENT '退款状态 0处理中 1成功 2失败',
  `refund_no`       varchar(64)  DEFAULT NULL COMMENT '支付宝退款单号',
  `operator`        bigint(20)   DEFAULT NULL COMMENT '操作人(管理员ID)',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_id` (`payment_id`),
  KEY `idx_order_sn` (`order_sn`),
  KEY `idx_refund_status` (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录';

-- 本地事务表（Saga 可靠性保证 — 支付事件发布）
CREATE TABLE IF NOT EXISTS local_transaction (
  `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_id`    varchar(64) NOT NULL COMMENT '消息ID(全局唯一)',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型(payment.success/payment.fail/refund.success)',
  `business_key`  varchar(100) NOT NULL COMMENT '业务主键(order_sn)',
  `status`        tinyint(4)  NOT NULL DEFAULT '0' COMMENT '状态 0INIT 1DONE 2FAIL',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地事务表(Saga — 支付事件)';
