-- ========================================
-- FlashFlow 演示数据初始化脚本
-- ========================================
-- 使用方式:
--   mysql -uroot -pzph flashflow_inventory < sql/init-demo-data.sql
-- 或登录 MySQL 后:
--   source D:/javacode/Java/Pro/resume_pro/flashflow/sql/init-demo-data.sql
-- ========================================
-- 导入后需执行 Redis 预热:
--   POST /api/flashflow/promotion/activity/{id}/publish
-- 或通过前端活动管理页面点击 "发布"
-- ========================================

-- ========================================
-- 1. 库存数据 (flashflow_inventory)
-- ========================================

-- 清空旧数据
DELETE FROM inventory_log;
DELETE FROM inventory_shard;
DELETE FROM inventory;

-- 商品 1: iPhone 15 Pro (库存 100)
INSERT INTO inventory (sku_id, total_stock, shard_count, version) VALUES (1001, 100, 16, 0);
INSERT INTO inventory_shard (sku_id, shard_index, shard_stock, frozen_stock) VALUES
(1001, 0, 6, 0), (1001, 1, 6, 0), (1001, 2, 6, 0), (1001, 3, 6, 0),
(1001, 4, 6, 0), (1001, 5, 6, 0), (1001, 6, 6, 0), (1001, 7, 6, 0),
(1001, 8, 6, 0), (1001, 9, 6, 0), (1001, 10, 6, 0), (1001, 11, 6, 0),
(1001, 12, 6, 0), (1001, 13, 6, 0), (1001, 14, 6, 0), (1001, 15, 10, 0);

-- 商品 2: MacBook Air M3 (库存 50)
INSERT INTO inventory (sku_id, total_stock, shard_count, version) VALUES (1002, 50, 16, 0);
INSERT INTO inventory_shard (sku_id, shard_index, shard_stock, frozen_stock) VALUES
(1002, 0, 3, 0), (1002, 1, 3, 0), (1002, 2, 3, 0), (1002, 3, 3, 0),
(1002, 4, 3, 0), (1002, 5, 3, 0), (1002, 6, 3, 0), (1002, 7, 3, 0),
(1002, 8, 3, 0), (1002, 9, 3, 0), (1002, 10, 3, 0), (1002, 11, 3, 0),
(1002, 12, 3, 0), (1002, 13, 3, 0), (1002, 14, 3, 0), (1002, 15, 5, 0);

-- 商品 3: AirPods Pro 2 (库存 200)
INSERT INTO inventory (sku_id, total_stock, shard_count, version) VALUES (1003, 200, 16, 0);
INSERT INTO inventory_shard (sku_id, shard_index, shard_stock, frozen_stock) VALUES
(1003, 0, 12, 0), (1003, 1, 12, 0), (1003, 2, 12, 0), (1003, 3, 12, 0),
(1003, 4, 12, 0), (1003, 5, 12, 0), (1003, 6, 12, 0), (1003, 7, 12, 0),
(1003, 8, 12, 0), (1003, 9, 12, 0), (1003, 10, 12, 0), (1003, 11, 12, 0),
(1003, 12, 12, 0), (1003, 13, 12, 0), (1003, 14, 12, 0), (1003, 15, 20, 0);

-- 商品 4: Apple Watch S9 (库存 80)
INSERT INTO inventory (sku_id, total_stock, shard_count, version) VALUES (1004, 80, 16, 0);
INSERT INTO inventory_shard (sku_id, shard_index, shard_stock, frozen_stock) VALUES
(1004, 0, 5, 0), (1004, 1, 5, 0), (1004, 2, 5, 0), (1004, 3, 5, 0),
(1004, 4, 5, 0), (1004, 5, 5, 0), (1004, 6, 5, 0), (1004, 7, 5, 0),
(1004, 8, 5, 0), (1004, 9, 5, 0), (1004, 10, 5, 0), (1004, 11, 5, 0),
(1004, 12, 5, 0), (1004, 13, 5, 0), (1004, 14, 5, 0), (1004, 15, 5, 0);

-- 商品 5: iPad Air (库存 60)
INSERT INTO inventory (sku_id, total_stock, shard_count, version) VALUES (1005, 60, 16, 0);
INSERT INTO inventory_shard (sku_id, shard_index, shard_stock, frozen_stock) VALUES
(1005, 0, 3, 0), (1005, 1, 3, 0), (1005, 2, 3, 0), (1005, 3, 3, 0),
(1005, 4, 3, 0), (1005, 5, 3, 0), (1005, 6, 3, 0), (1005, 7, 3, 0),
(1005, 8, 3, 0), (1005, 9, 3, 0), (1005, 10, 3, 0), (1005, 11, 3, 0),
(1005, 12, 3, 0), (1005, 13, 3, 0), (1005, 14, 3, 0), (1005, 15, 15, 0);


-- ========================================
-- 2. 秒杀活动数据 (flashflow_promotion)
-- ========================================

-- 清空旧数据
DELETE FROM promotion_record;
DELETE FROM promotion_sku;
DELETE FROM promotion_activity;

-- 活动 1: 周年庆秒杀（进行中）
INSERT INTO promotion_activity (id, activity_type, name, start_time, end_time, status, remark)
VALUES (1, 'FLASH_SALE', '周年庆秒杀狂欢',
        DATE_SUB(NOW(), INTERVAL 30 MINUTE),
        DATE_ADD(NOW(), INTERVAL 2 HOUR),
        2, '周年庆限时秒杀，全场低至5折');

-- 活动 2: 新品首发（即将开始）
INSERT INTO promotion_activity (id, activity_type, name, start_time, end_time, status, remark)
VALUES (2, 'FLASH_SALE', '新品首发限量抢',
        DATE_ADD(NOW(), INTERVAL 1 DAY),
        DATE_ADD(NOW(), INTERVAL 1 DAY + INTERVAL 3 HOUR),
        1, '新品首发，限量抢购');

-- 活动 3: 618 年中大促（已结束）
INSERT INTO promotion_activity (id, activity_type, name, start_time, end_time, status, remark)
VALUES (3, 'FLASH_SALE', '618 年中大促',
        DATE_SUB(NOW(), INTERVAL 7 DAY),
        DATE_SUB(NOW(), INTERVAL 6 DAY),
        3, '618 年中大促活动已结束');


-- ========================================
-- 3. 活动商品数据 (flashflow_promotion)
-- ========================================

-- 活动 1 商品（进行中）
INSERT INTO promotion_sku (activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort)
VALUES
(1, 1001, 'iPhone 15 Pro 256GB', '/assets/products/iphone15.jpg', 9999.00, 6999.00, 100, 1, 0, 1),
(1, 1002, 'MacBook Air M3 16GB', '/assets/products/macbook-air.jpg', 12999.00, 8999.00, 50, 1, 0, 2),
(1, 1003, 'AirPods Pro 2', '/assets/products/airpods-pro.jpg', 1999.00, 1299.00, 200, 2, 0, 3),
(1, 1004, 'Apple Watch S9', '/assets/products/apple-watch.jpg', 3499.00, 2499.00, 80, 1, 0, 4),
(1, 1005, 'iPad Air M2', '/assets/products/ipad-air.jpg', 4999.00, 3699.00, 60, 1, 0, 5);

-- 活动 2 商品（即将开始）
INSERT INTO promotion_sku (activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort)
VALUES
(2, 1001, 'iPhone 15 Pro 256GB', '/assets/products/iphone15.jpg', 9999.00, 7499.00, 50, 1, 0, 1),
(2, 1003, 'AirPods Pro 2', '/assets/products/airpods-pro.jpg', 1999.00, 1499.00, 100, 2, 0, 2);

-- 活动 3 商品（已结束，带销量）
INSERT INTO promotion_sku (activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort)
VALUES
(3, 1001, 'iPhone 15 Pro 256GB', '/assets/products/iphone15.jpg', 9999.00, 6599.00, 200, 1, 200, 1),
(3, 1002, 'MacBook Air M3 16GB', '/assets/products/macbook-air.jpg', 12999.00, 8499.00, 100, 1, 100, 2),
(3, 1003, 'AirPods Pro 2', '/assets/products/airpods-pro.jpg', 1999.00, 1199.00, 500, 2, 500, 3);


-- ========================================
-- 4. 系统用户演示账号 (flashflow_auth)
-- ========================================
-- 注意: 以下密码使用 BCrypt 加密，密码为 "admin123"
-- 管理员账号已由 init-auth.sql 创建
-- C 端用户注册通过 API: POST /api/flashflow/auth/user/register
INSERT INTO user_info (phone, password, nickname, status, create_time)
SELECT * FROM (
  SELECT '13800138001' AS phone,
         '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' AS password,
         '秒杀用户01' AS nickname, 1 AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM user_info WHERE phone = '13800138001');

INSERT INTO user_info (phone, password, nickname, status, create_time)
SELECT * FROM (
  SELECT '13800138002' AS phone,
         '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' AS password,
         '秒杀用户02' AS nickname, 1 AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM user_info WHERE phone = '13800138002');

INSERT INTO user_info (phone, password, nickname, status, create_time)
SELECT * FROM (
  SELECT '13800138003' AS phone,
         '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' AS password,
         '测试用户' AS nickname, 1 AS status, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM user_info WHERE phone = '13800138003');

-- ========================================
-- 5. 使用说明
-- ========================================
-- 导入此 SQL 后需执行 Redis 预热:
-- 方法1 (API): 对每个活动调用 POST /api/flashflow/promotion/activity/{id}/publish
-- 方法2 (前端): 登录后 → 活动管理 → 点击活动1 "发布" 按钮
--
-- 演示账号:
--   管理员: admin / Admin@123
--   C端用户: 13800138001 / admin123 (手机号/密码)
--   C端用户: 13800138002 / admin123
--   C端用户: 13800138003 / admin123
