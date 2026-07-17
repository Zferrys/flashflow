-- 扣库存脚本（原子操作）
-- KEYS[1] = stock:{skuId}:{shard}
-- ARGV[1] = 扣减数量
-- return 1=成功 0=库存不足

local stock = redis.call('GET', KEYS[1])
if not stock then
    return 0
end
stock = tonumber(stock)
if stock >= tonumber(ARGV[1]) then
    redis.call('DECRBY', KEYS[1], ARGV[1])
    return 1
else
    return 0
end
