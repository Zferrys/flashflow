USE flashflow_promotion;

DELETE FROM promotion_record;
DELETE FROM promotion_sku;
DELETE FROM promotion_activity;

-- 活动数据（使用 init-product.sql 中存在的 SKU ID：1001-1005, 2001-2002）
INSERT INTO promotion_activity (id, activity_type, name, start_time, end_time, status, remark) VALUES
(1, 'FLASH_SALE', '周年庆秒杀狂欢', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 0, '周年庆限时秒杀'),
(2, 'FLASH_SALE', '新品首发限量抢', DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), 1, '新品首发'),
(3, 'FLASH_SALE', '618 年中大促', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 3, '618已结束');

-- sku_id 已对齐到 init-product.sql 中的真实 ID
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES
(1, 1, 1001, 'iPhone 15 Pro 128G 深空黑', '/assets/products/iphone15-pro.svg', 7999.00, 6299.00, 100, 1, 0, 1, NOW()),
(2, 1, 2001, 'MacBook Air M3 8G+256G 午夜色', '/assets/products/macbook-air.svg', 8499.00, 7999.00, 50, 1, 0, 2, NOW()),
(3, 1, 1003, 'AirPods Pro 2 USB-C', '/assets/products/airpods-pro.svg', 1899.00, 1299.00, 200, 2, 0, 3, NOW()),
(4, 1, 1004, 'Apple Watch S9 GPS 45mm', '/assets/products/apple-watch.jpg', 3199.00, 2299.00, 80, 1, 0, 4, NOW()),
(5, 1, 1005, 'iPad Air 11英寸 M2', '/assets/products/ipad-air.jpg', 4799.00, 2499.00, 60, 1, 0, 5, NOW()),
(6, 2, 2002, 'MacBook Air M3 16G+512G 星光色', '/assets/products/macbook-pro.svg', 10499.00, 11999.00, 50, 1, 0, 1, NOW()),
(7, 2, 1002, 'iPhone 15 Pro 256G 银色', '/assets/products/iphone15.svg', 8999.00, 7999.00, 100, 1, 0, 2, NOW()),
(8, 3, 1002, 'iPhone 15 Pro 256G 银色', '/assets/products/iphone15.svg', 8999.00, 6599.00, 200, 1, 200, 1, NOW()),
(9, 3, 2002, 'MacBook Air M3 16G+512G 星光色', '/assets/products/macbook-pro.svg', 10499.00, 10999.00, 100, 1, 100, 2, NOW()),
(10, 3, 1003, 'AirPods Pro 2 USB-C', '/assets/products/airpods-pro.svg', 1899.00, 1199.00, 500, 2, 500, 3, NOW());
