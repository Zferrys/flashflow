USE flashflow_promotion;

DELETE FROM promotion_record;
DELETE FROM promotion_sku;
DELETE FROM promotion_activity;

INSERT INTO promotion_activity (id, activity_type, name, start_time, end_time, status, remark) VALUES
(1, 'FLASH_SALE', '周年庆秒杀狂欢', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 0, '周年庆限时秒杀'),
(2, 'FLASH_SALE', '新品首发限量抢', DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), 1, '新品首发'),
(3, 'FLASH_SALE', '618 年中大促', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 3, '618已结束');

INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (1,1,17,'iPhone 16 Pro 128GB','/assets/products/sku-17.jpg',8999.00,6299.00,100,1,0,1,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (2,1,5,'MacBook Air M3 16GB/256GB','/assets/products/sku-5.jpg',10999.00,7999.00,50,1,0,2,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (3,1,7,'AirPods Pro 2 USB-C','/assets/products/sku-7.jpg',1899.00,1299.00,200,2,0,3,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (4,1,8,'Apple Watch Series 9 45mm','/assets/products/sku-8.jpg',3199.00,2299.00,80,1,0,4,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (5,1,36,'iPad 10代 64GB','/assets/products/sku-36.jpg',3499.00,2499.00,60,1,0,5,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (6,2,26,'MacBook Pro 14 M4 18GB/512GB','/assets/products/sku-26.jpg',14999.00,11999.00,50,1,0,1,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (7,2,34,'iPad Pro M4 13 256GB','/assets/products/sku-34.jpg',9999.00,7999.00,100,1,0,2,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (8,3,17,'iPhone 16 Pro 128GB','/assets/products/sku-17.jpg',8999.00,6599.00,200,1,200,1,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (9,3,26,'MacBook Pro 14 M4 18GB/512GB','/assets/products/sku-26.jpg',14999.00,10999.00,100,1,100,2,NOW());
INSERT INTO promotion_sku (id, activity_id, sku_id, sku_name, sku_image, original_price, activity_price, stock_limit, per_user_limit, sold_count, sort, create_time) VALUES (10,3,7,'AirPods Pro 2 USB-C','/assets/products/sku-7.jpg',1899.00,1199.00,500,2,500,3,NOW());
