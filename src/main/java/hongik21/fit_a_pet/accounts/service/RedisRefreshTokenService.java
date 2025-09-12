package hongik21.fit_a_pet.accounts.service;

import hongik21.fit_a_pet.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    // 리프레시 토큰 유효기간  : 14일
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 14;
    private final JwtProvider jwtProvider;

    // 리프레시 토큰을 redis에 저장
    // key: "refreshToken:{memberId}", value: "token"
    //     * @param memberId
    //     * @param refreshToken
    public void save(Long memberId, String refreshToken) {
        log.info("Redis에 리프레시 토큰 저장 시작: memberId={}, token={}", memberId, refreshToken);
        String key = "refreshToken:" + memberId;
        redisTemplate.opsForValue().set(key, refreshToken,REFRESH_TOKEN_EXPIRATION_DAYS, TimeUnit.DAYS);
        log.info("Redis에 리프레시 토큰 저장 완료");
    }

    // redis에서 리프레시 토큰을 가져옴
    //     @param memberId
    //     @return
    public String getToken(Long memberId) {
        String key = "refreshToken:" + memberId;
        return redisTemplate.opsForValue().get(key);
    }

    // Redis에서 리프레시 토큰을 삭제합니다. (로그아웃 시)
    // @param memberId
    public void delete(Long memberId) {
        String key = "refreshToken:" + memberId;
        redisTemplate.delete(key);
    }

    // access token을 blacklist에 추가
    //    key: "blacklist:{accessToken}", value: "logout"
    //          @param accessToken
    public void addToBlacklist(String accessToken) {
        String key = "blacklist:" + accessToken;
        // access token의 남은 유효시간 동안 저장
        long expirationTime = jwtProvider.getExpirationTime(accessToken);
        if (expirationTime < 0) {
            redisTemplate.opsForValue().set(key, "logout",expirationTime,TimeUnit.MILLISECONDS);
        }
    }

    // 토큰이 블랙리스트에 있는지 확인
    //    @param accessToken
    //    @return 블랙리스트에 있으면 true, 없으면 false
    public boolean isTokenBlacklisted(String accessToken) {
        String key = "blacklist:" + accessToken;
        return redisTemplate.hasKey(key);
    }


}
