package hongik21.fit_a_pet.accounts.service;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.accounts.repository.RedisSessionStore;
import hongik21.fit_a_pet.global.jwt.*;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtProvider jwt;
    private final RedisSessionStore sessions;
    private final MemberRepository members;
    private final RedisRefreshTokenService blacklist;
    public TokenDto login(Member member) {
        String sid = UUID.randomUUID().toString();
        long expires = jwt.newSessionExpiration();
        TokenDto pair = jwt.createToken(member, sid, expires);
        if (!sessions.create(sid, pair.refreshToken(), expires)) throw new IllegalStateException("Session creation failed");
        return pair;
    }
    public TokenDto refresh(String token) {
        Claims c = jwt.parse(token, "refresh");
        Member member = members.findByEmail(c.getSubject()).orElseThrow(() -> new JwtException("Invalid member"));
        String sid = c.get("sid", String.class);
        TokenDto pair = jwt.createToken(member, sid, c.getExpiration().getTime());
        long result = sessions.rotate(sid, token, pair.refreshToken());
        if (result == -1) log.warn("Refresh replay detected; session revoked: sid={}", sid);
        if (result != 1) throw new JwtException("Invalid session");
        return pair;
    }
    public void logout(String token) {
        if (token == null) throw new JwtException("Missing token");
        Claims c = jwt.parseForLogout(token);
        sessions.revoke(c.get("sid", String.class));
        blacklist.addToBlacklist(token);
    }
}
