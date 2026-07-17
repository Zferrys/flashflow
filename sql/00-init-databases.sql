-- FlashFlow 数据库初始化
-- 创建所有业务库，各微服务连接各自的库

CREATE DATABASE IF NOT EXISTS `flashflow_auth`       DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `flashflow_promotion`  DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `flashflow_order`      DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `flashflow_inventory`  DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `flashflow_payment`    DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 各库完整 DDL 见需求文档第 11 节
