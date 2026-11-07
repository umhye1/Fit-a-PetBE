package hongik21.fit_a_pet.security;

import hongik21.fit_a_pet.IntegrationSupport;
import hongik21.fit_a_pet.accounts.entity.*;
import hongik21.fit_a_pet.accounts.repository.*;
import hongik21.fit_a_pet.accounts.service.*;
import hongik21.fit_a_pet.global.jwt.*;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class SessionIntegrationTest extends IntegrationSupport {
    @Autowired TokenService tokens;
    @Autowired JwtProvider jwt;
    @Autowired RedisSessionStore sessions;
    @Autowired RedisRefreshTokenService blacklist;
    @Autowired MemberRepository members;
    @Autowired StringRedisTemplate redis;
    @Autowired BCryptPasswordEncoder passwords;
    @Autowired MockMvc mvc;
    Member member;
    @BeforeEach void setup() {
        member = members.save(Member.builder().name("synthetic").email(UUID.randomUUID().toString().replace("-", "")+"@test.com")
                .nickname("test").password(passwords.encode("test-password"))
                .memberType(MemberType.GENERAL).createdAt(LocalDateTime.now()).build());
    }
    String sid(TokenDto pair) { return jwt.parse(pair.accessToken(),"access").get("sid", String.class); }
    void protectedStatus(String token, int status) throws Exception {
        mvc.perform(get("/api/users/test").header("Authorization","Bearer "+token)).andExpect(status().is(status));
    }
    @Test void loginRefreshLogoutAndBlacklistTtl() throws Exception {
        TokenDto pair=tokens.login(member);
        protectedStatus(pair.accessToken(),200);
        tokens.logout(pair.accessToken());
        protectedStatus(pair.accessToken(),401);
        assertThrows(JwtException.class,()->tokens.refresh(pair.refreshToken()));
        long ttl=redis.getExpire("blacklist:"+RedisSessionStore.hash(pair.accessToken()),TimeUnit.MILLISECONDS);
        assertTrue(ttl>0 && ttl<=60000);
        tokens.logout(pair.accessToken()); // idempotent
        assertFalse(sessions.active(sid(pair)));
    }
    @Test void httpLoginRefreshAndLogoutUseTheSameSessionContract() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        String login=mvc.perform(post("/api/users/login").contentType("application/json")
                .content("{\"email\":\""+member.getEmail()+"\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var first=mapper.readTree(login).get("data");
        String next=mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("refreshToken",first.get("refreshToken").asText()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String access=mapper.readTree(next).get("data").get("accessToken").asText();
        protectedStatus(access,200);
        mvc.perform(post("/api/users/logout").header("Authorization","Bearer "+access)).andExpect(status().isOk());
        protectedStatus(access,401);
    }
    @Test void purposeConfusionAndMissingCredentialsAreRejected() throws Exception {
        TokenDto pair=tokens.login(member);
        protectedStatus(pair.refreshToken(),401);
        mvc.perform(get("/api/users/test")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content("{\"refreshToken\":\""+pair.accessToken()+"\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        assertTrue(sessions.active(sid(pair)));
    }
    @Test void replayRevokesDescendantsAndAccessButOtherDeviceSurvives() throws Exception {
        TokenDto original=tokens.login(member), other=tokens.login(member);
        TokenDto rotated=tokens.refresh(original.refreshToken());
        protectedStatus(rotated.accessToken(),200);
        assertThrows(JwtException.class,()->tokens.refresh(original.refreshToken()));
        protectedStatus(original.accessToken(),401);
        protectedStatus(rotated.accessToken(),401);
        assertThrows(JwtException.class,()->tokens.refresh(rotated.refreshToken()));
        protectedStatus(other.accessToken(),200);
        assertNotNull(tokens.refresh(other.refreshToken()));
    }
    @Test void loginRotationAndLogoutNeverLogRawTokens(org.springframework.boot.test.system.CapturedOutput output) {
        TokenDto pair=tokens.login(member);
        TokenDto next=tokens.refresh(pair.refreshToken());tokens.logout(next.accessToken());
        for(String raw:List.of(pair.accessToken(),pair.refreshToken(),next.accessToken(),next.refreshToken()))
            assertFalse(output.getAll().contains(raw));
    }
    @Test void invalidUnconsumedHashDoesNotRevokeSession() {
        TokenDto pair=tokens.login(member);
        assertEquals(0,sessions.rotate(sid(pair),"not-issued","other"));
        assertTrue(sessions.active(sid(pair)));
    }
    @ParameterizedTest @ValueSource(ints={2,10,100})
    void sameRefreshHasExactlyOneWinnerThenRevokesFamily(int count) throws Exception {
        for(int iteration=0;iteration<3;iteration++) {
            TokenDto pair=tokens.login(member);
            AtomicInteger successes=new AtomicInteger();
            Queue<TokenDto> returned=new ConcurrentLinkedQueue<>();
            runTogether(count, () -> {
                try { returned.add(tokens.refresh(pair.refreshToken())); successes.incrementAndGet(); }
                catch(JwtException expected) { }
            });
            assertEquals(1,successes.get());
            assertFalse(sessions.active(sid(pair)));
            for(TokenDto next: returned) { protectedStatus(next.accessToken(),401);
                assertThrows(JwtException.class,()->tokens.refresh(next.refreshToken())); }
        }
    }
    @Test void logoutRacingRefreshNeverResurrectsSession() throws Exception {
        for(int i=0;i<20;i++) {
            TokenDto pair=tokens.login(member);
            Queue<TokenDto> returned=new ConcurrentLinkedQueue<>();
            var barrier=new CyclicBarrier(2);
            try(var pool=Executors.newFixedThreadPool(2)) {
                Future<?> a=pool.submit(()->{ await(barrier);tokens.logout(pair.accessToken()); });
                Future<?> b=pool.submit(()->{ await(barrier);try{returned.add(tokens.refresh(pair.refreshToken()));}catch(JwtException expected){} });
                a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
            }
            assertFalse(sessions.active(sid(pair)));
            for(var next:returned) protectedStatus(next.accessToken(),401);
        }
    }
    @Test void expiredStateCannotBeRefreshedAndConsumedHistoryKeepsAbsoluteTtl() throws Exception {
        TokenDto pair=tokens.login(member);
        String key=RedisSessionStore.key(sid(pair));
        long before=redis.getExpire(key,TimeUnit.MILLISECONDS);
        TokenDto next=tokens.refresh(pair.refreshToken());
        long after=redis.getExpire(key,TimeUnit.MILLISECONDS);
        assertTrue(after>0 && after<=before);
        assertTrue(redis.opsForHash().hasKey(key,"used:"+RedisSessionStore.hash(pair.refreshToken())));
        redis.expire(key,java.time.Duration.ofMillis(30));
        Thread.sleep(60);
        assertFalse(sessions.active(sid(pair)));
        assertThrows(JwtException.class,()->tokens.refresh(next.refreshToken()));
    }
    static void runTogether(int count,Runnable action) throws Exception {
        var barrier=new CyclicBarrier(count);
        try(var pool=Executors.newFixedThreadPool(count)) {
            var tasks=new ArrayList<Future<?>>();
            for(int i=0;i<count;i++)tasks.add(pool.submit(()->{await(barrier);action.run();}));
            for(var f:tasks)f.get(20,TimeUnit.SECONDS);
        }
    }
    static void await(CyclicBarrier barrier) {
        try{barrier.await(10,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}
    }
}
