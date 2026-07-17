-- 释放预扣库存脚本（幂等原子操作）
-- KEYS[1] = stock:{skuId}:{shard}
-- ARGV[1] = 释放数量
-- return 1=成功（key 不存在时视为已释放，直接返回成功）

if redis.call('EXISTS', KEYS[1]) == 0 then
    return 1
end
redis.call('INCRBY', KEYS[1], ARGV[1])
return 1
