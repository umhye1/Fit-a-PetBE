package hongik21.fit_a_pet.security;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.accounts.service.RedisRefreshTokenService;
import hongik21.fit_a_pet.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BaselineRegressionTest {
    @Test void validAccessTokenMustBeBlacklistedForRemainingLifetime() {
        @SuppressWarnings("unchecked") RedisTemplate<String,String> redis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        JwtProvider jwt = mock(JwtProvider.class);
        when(jwt.getExpirationTime("valid-access")).thenReturn(60000L);
        new RedisRefreshTokenService(redis, jwt).addToBlacklist("valid-access");
        verify(values).set(anyString(), eq("logout"), eq(60000L), eq(TimeUnit.MILLISECONDS));
    }
    @Test void expiredTokenDoesNotCreateANegativeTtlEntry() {
        @SuppressWarnings("unchecked") RedisTemplate<String,String> redis = mock(RedisTemplate.class);
        JwtProvider jwt = mock(JwtProvider.class);
        when(jwt.getExpirationTime("expired")).thenReturn(-1000L);
        new RedisRefreshTokenService(redis, jwt).addToBlacklist("expired");
        verifyNoInteractions(redis);
    }
    @Test void refreshTokenMustNotAuthenticateAsAccessToken() {
        MemberRepository members = mock(MemberRepository.class);
        JwtProvider jwt = new JwtProvider();
        ReflectionTestUtils.setField(jwt,"secret","test-secret-only-at-least-32-bytes-long-123456789");
        ReflectionTestUtils.setField(jwt,"accessTokenExpiration",60000L);
        ReflectionTestUtils.setField(jwt,"refreshTokenExpiration",120000L);
        ReflectionTestUtils.setField(jwt,"issuer","fit-a-pet");
        ReflectionTestUtils.setField(jwt,"audience","fit-a-pet-api");
        jwt.init();
        assertFalse(jwt.validateAccessToken(jwt.createToken(Member.builder().email("a@example.com").build(), java.util.UUID.randomUUID().toString(), jwt.newSessionExpiration()).refreshToken()));
    }
}
