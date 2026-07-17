-- 用户限购计数脚本（原子自增+检查）
-- KEYS[1] = flashflow:promotion:buy_limit:{activityId}:{userId}
-- ARGV[1] = 限购数量
-- ARGV[2] = TTL（秒）
-- return 1=允许购买 0=已达限购上限

local count = redis.call('INCR', KEYS[1])
if count == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
if count <= tonumber(ARGV[1]) then
    return 1
else
    return 0
end
