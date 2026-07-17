-- 限购检查脚本（原子操作）
-- KEYS[1] = flashflow:promotion:buy_limit:{activityId}:{userId}
-- ARGV[1] = 限购数量（perUserLimit）
-- ARGV[2] = TTL（秒）
-- return 1=允许购买 0=超过限购

local bought = redis.call('GET', KEYS[1])
if not bought then
    redis.call('SETEX', KEYS[1], ARGV[2], 1)
    return 1
end

bought = tonumber(bought)
if bought >= tonumber(ARGV[1]) then
    return 0
end

redis.call('SET', KEYS[1], bought + 1)
redis.call('EXPIRE', KEYS[1], ARGV[2])
return 1
