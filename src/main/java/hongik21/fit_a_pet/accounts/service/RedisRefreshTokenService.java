package hongik21.fit_a_pet.accounts.service;

import hongik21.fit_a_pet.accounts.repository.RedisSessionStore;
import hongik21.fit_a_pet.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRefreshTokenService {
    private final RedisTemplate<String,String> redisTemplate;
    private final JwtProvider jwtProvider;
    public RedisRefreshTokenService(@Qualifier("stringRedisTemplate") RedisTemplate<String,String> redisTemplate, JwtProvider jwtProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtProvider = jwtProvider;
    }
    public void addToBlacklist(String accessToken) {
        long expirationTime = jwtProvider.getExpirationTime(accessToken);
        if (expirationTime > 0) redisTemplate.opsForValue().set(
                "blacklist:" + RedisSessionStore.hash(accessToken), "logout", expirationTime, TimeUnit.MILLISECONDS);
    }
    public boolean isTokenBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + RedisSessionStore.hash(accessToken)));
    }
}
