-- FlashFlow 优惠券适用范围索引优化
-- 新增 scope_sku_id 辅助列，替代 JSON_CONTAINS 全表扫描，走索引查询

ALTER TABLE coupon ADD COLUMN scope_sku_id BIGINT DEFAULT NULL COMMENT '适用SKU_ID(冗余索引列)' AFTER scope_value;
CREATE INDEX idx_scope_sku ON coupon (scope_sku_id);

-- 回填已有数据：SKU 类型的 scope_value 如 '[7]'，提取第一个数字
UPDATE coupon SET scope_sku_id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(scope_value, '[', -1), ']', 1) AS UNSIGNED)
WHERE scope = 'SKU' AND scope_value IS NOT NULL AND scope_value != '';

-- 注意：多 SKU 共享一张券（scope_value='[7,8,9]'）不走此列，继续用 scope 表关联查询
-- 单 SKU 场景覆盖了 95% 的实际业务
