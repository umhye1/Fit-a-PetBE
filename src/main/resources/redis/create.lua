-- All validation precedes writes; one hash preserves state and consumed history TTL.
local expires = tonumber(ARGV[2])
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
if not expires or expires <= now or string.len(ARGV[1]) ~= 64 then return 0 end
if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
redis.call('HSET', KEYS[1], 'state', 'active', 'current', ARGV[1], 'expires', ARGV[2])
redis.call('PEXPIREAT', KEYS[1], expires)
return 1
