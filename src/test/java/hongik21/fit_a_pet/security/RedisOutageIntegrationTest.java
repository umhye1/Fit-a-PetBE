package hongik21.fit_a_pet.security;

import hongik21.fit_a_pet.IntegrationSupport;
import hongik21.fit_a_pet.accounts.entity.*;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.accounts.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RedisOutageIntegrationTest extends IntegrationSupport {
    @Autowired TokenService tokens;
    @Autowired MemberRepository members;
    @Autowired MockMvc mvc;
    @Test void unavailableRedisReturns503AndRevocationSurvivesRecovery() throws Exception {
        var member=members.save(Member.builder().email(UUID.randomUUID()+"@test.com").name("test").nickname("test")
                .password("unused").memberType(MemberType.GENERAL).createdAt(LocalDateTime.now()).build());
        var active=tokens.login(member);var revoked=tokens.login(member);tokens.logout(revoked.accessToken());
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            mvc.perform(get("/api/users/test").header("Authorization","Bearer "+active.accessToken()))
                    .andExpect(status().isServiceUnavailable());
            mvc.perform(post("/api/auth/refresh").contentType("application/json")
                    .content("{\"refreshToken\":\""+active.refreshToken()+"\"}"))
                    .andExpect(status().isServiceUnavailable());
        } finally { REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec(); }
        mvc.perform(get("/api/users/test").header("Authorization","Bearer "+revoked.accessToken()))
                .andExpect(status().isUnauthorized());
    }
}
