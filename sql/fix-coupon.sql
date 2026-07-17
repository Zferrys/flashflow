-- ========================================
-- FlashFlow 优惠券表升级 + 新演示数据
-- ========================================
USE flashflow_promotion;

-- 1. 新增字段（如已存在会报错，先检查）
ALTER TABLE coupon
  ADD COLUMN `scope` varchar(20) NOT NULL DEFAULT 'ALL' COMMENT '范围:ALL=全场,CATEGORY=分类,SKU=指定商品' AFTER `status`,
  ADD COLUMN `scope_value` varchar(500) DEFAULT NULL COMMENT '范围值:分类ID或SKU ID JSON数组' AFTER `scope`,
  ADD COLUMN `auto_grant` varchar(20) NOT NULL DEFAULT 'NONE' COMMENT '自动发放:NONE=手动,NEW_USER=新用户,FIRST_ORDER=首单' AFTER `scope_value`;

-- 2. 清空旧演示数据
TRUNCATE TABLE user_coupon;
DELETE FROM coupon;

-- 3. 新的演示优惠券（绑定真实品类和商品）
INSERT INTO coupon (id, name, type, condition_amount, discount_amount, discount_rate, total_count, remain_count, per_user_limit, start_time, end_time, status, scope, scope_value, auto_grant) VALUES
-- 全场券
(1, '新用户注册礼',       1, 0.00,    20.00,   NULL, 1000, 1000, 1, '2025-01-01', '2027-12-31', 1, 'ALL',      NULL,     'NEW_USER'),
(2, '618大促满减券',       1, 500.00,  66.00,   NULL, 500,  500,  1, '2026-06-01', '2026-06-30', 1, 'ALL',      NULL,     'NONE'),
(3, '全场95折券',          2, 0.00,    NULL,    0.95, 200,  200,  1, '2025-01-01', '2027-12-31', 1, 'ALL',      NULL,     'NONE'),
-- 分类券
(4, '手机数码专享券',      1, 2000.00, 150.00,  NULL, 500,  500,  1, '2025-06-01', '2027-12-31', 1, 'CATEGORY', '1',      'NONE'),
(5, '笔记本满减券',        1, 8000.00, 500.00,  NULL, 200,  200,  1, '2025-06-01', '2027-12-31', 1, 'CATEGORY', '2',      'NONE'),
(6, '配件数码专享券',      1, 99.00,   30.00,   NULL, 300,  300,  2, '2025-01-01', '2027-12-31', 1, 'CATEGORY', '3',      'NONE'),
-- 单品券
(7, 'AirPods 专属券',      1, 99.00,   50.00,   NULL, 100,  100,  1, '2025-01-01', '2027-12-31', 1, 'SKU',      '[1003]',    'NONE'),
(8, 'iPhone 15 Pro 专属券',1, 5000.00, 300.00,  NULL, 100,  100,  1, '2025-01-01', '2027-12-31', 1, 'SKU',      '[1001]',   'NONE');
