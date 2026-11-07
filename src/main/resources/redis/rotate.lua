if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
if redis.call('TYPE', KEYS[1]).ok ~= 'hash' then return 0 end
local expires = tonumber(redis.call('HGET', KEYS[1], 'expires'))
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
if not expires or expires <= now or string.len(ARGV[1]) ~= 64 or string.len(ARGV[2]) ~= 64 then return 0 end
if redis.call('HGET', KEYS[1], 'state') ~= 'active' then return 0 end
if redis.call('HEXISTS', KEYS[1], 'used:' .. ARGV[1]) == 1 then
    redis.call('HSET', KEYS[1], 'state', 'revoked')
    return -1
end
if redis.call('HGET', KEYS[1], 'current') ~= ARGV[1] then return 0 end
redis.call('HSET', KEYS[1], 'used:' .. ARGV[1], '1', 'current', ARGV[2])
return 1
