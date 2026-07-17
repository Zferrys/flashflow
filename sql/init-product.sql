-- FlashFlow 商品基础数据（SPU/SKU/分类/品牌）
-- 导入前确保 flashflow_promotion 库已存在

CREATE DATABASE IF NOT EXISTS flashflow_promotion DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE flashflow_promotion;

-- 商品分类（三级）
CREATE TABLE IF NOT EXISTS product_category (
  `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`   bigint(20)  NOT NULL DEFAULT '0' COMMENT '父分类ID(0为顶级)',
  `name`        varchar(50) NOT NULL COMMENT '分类名称',
  `level`       tinyint(4)  NOT NULL DEFAULT '1' COMMENT '层级 1级/2级/3级',
  `icon`        varchar(200) DEFAULT NULL COMMENT '分类图标',
  `sort`        int(11)     NOT NULL DEFAULT '0' COMMENT '排序号',
  `status`      tinyint(4)  NOT NULL DEFAULT '1' COMMENT '状态 1显示 0隐藏',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类（三级）';

-- 商品品牌
CREATE TABLE IF NOT EXISTS product_brand (
  `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        varchar(50) NOT NULL COMMENT '品牌名称',
  `logo`        varchar(500) DEFAULT NULL COMMENT '品牌LOGO',
  `sort`        int(11)     NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品品牌';

-- 商品SPU（标准化产品单元）
CREATE TABLE IF NOT EXISTS product_spu (
  `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_name`      varchar(200) NOT NULL COMMENT 'SPU名称',
  `category_id`   bigint(20)   DEFAULT NULL COMMENT '分类ID',
  `brand_id`      bigint(20)   DEFAULT NULL COMMENT '品牌ID',
  `description`   text         COMMENT '商品详情',
  `main_image`    varchar(500) DEFAULT NULL COMMENT '主图URL',
  `images`        json         COMMENT '轮播图列表',
  `status`        tinyint(4)   NOT NULL DEFAULT '0' COMMENT '状态 0下架 1上架',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU';

-- 商品SKU（库存持有单元）
CREATE TABLE IF NOT EXISTS product_sku (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键（即 sku_id，全系统引用）',
  `spu_id`      bigint(20)   NOT NULL COMMENT 'SPU_ID',
  `sku_name`    varchar(200) NOT NULL COMMENT 'SKU名称',
  `specs`       json         COMMENT '规格JSON',
  `price`       decimal(10,2) NOT NULL COMMENT '原价',
  `image`       varchar(500) DEFAULT NULL COMMENT 'SKU图片',
  `weight`      decimal(10,2) DEFAULT '0.00' COMMENT '重量(kg)',
  `status`      tinyint(4)   NOT NULL DEFAULT '0' COMMENT '状态 0下架 1上架',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_spu_id` (`spu_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU';

-- ======== 初始化商品数据 ========

INSERT INTO product_category (id, parent_id, name, level, sort) VALUES
(1, 0, '手机数码', 1, 1),
(2, 1, '手机', 2, 1),
(3, 1, '电脑', 2, 2),
(4, 0, '家用电器', 1, 2);

INSERT INTO product_brand (id, name, sort) VALUES
(1, 'Apple', 1),
(2, '华为', 2);

INSERT INTO product_spu (id, spu_name, category_id, brand_id, status) VALUES
(1, 'iPhone 15 Pro', 2, 1, 1),
(2, 'MacBook Air M3', 3, 1, 1),
(3, 'AirPods Pro 2', 2, 1, 1),
(4, 'Apple Watch S9', 2, 1, 1),
(5, 'iPad Air', 2, 1, 1);

INSERT INTO product_sku (id, spu_id, sku_name, price, status) VALUES
(1001, 1, 'iPhone 15 Pro 128G 深空黑', 7999.00, 1),
(1002, 1, 'iPhone 15 Pro 256G 银色', 8999.00, 1),
(1003, 3, 'AirPods Pro 2 USB-C', 1899.00, 1),
(1004, 4, 'Apple Watch S9 GPS 45mm', 3199.00, 1),
(1005, 5, 'iPad Air 11英寸 M2', 4799.00, 1),
(2001, 2, 'MacBook Air M3 8G+256G 午夜色', 8499.00, 1),
(2002, 2, 'MacBook Air M3 16G+512G 星光色', 10499.00, 1);
