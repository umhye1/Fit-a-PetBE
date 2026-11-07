package hongik21.fit_a_pet.accounts.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RedisSessionStore {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> CREATE = script("create");
    private static final DefaultRedisScript<Long> ROTATE = script("rotate");
    private static DefaultRedisScript<Long> script(String name) {
        var script = new DefaultRedisScript<Long>();
        script.setLocation(new ClassPathResource("redis/" + name + ".lua"));
        script.setResultType(Long.class);
        return script;
    }
    public static String key(String sid) { return "session:{" + sid + "}"; }
    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public boolean create(String sid, String token, long expiresAt) {
        return Long.valueOf(1).equals(redis.execute(CREATE, List.of(key(sid)), hash(token), Long.toString(expiresAt)));
    }
    public long rotate(String sid, String previous, String next) {
        Long result = redis.execute(ROTATE, List.of(key(sid)), hash(previous), hash(next));
        return result == null ? 0 : result;
    }
    public boolean active(String sid) { return "active".equals(redis.opsForHash().get(key(sid), "state")); }
    public void revoke(String sid) {
        // Deletion cannot resurrect a session even when racing with rotate.lua.
        redis.delete(key(sid));
    }
}
