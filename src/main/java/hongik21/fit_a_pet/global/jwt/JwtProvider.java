package hongik21.fit_a_pet.global.jwt;

import hongik21.fit_a_pet.accounts.entity.Member;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.access-token-expiration}") private long accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration}") private long refreshTokenExpiration;
    @Value("${jwt.issuer:fit-a-pet}") private String issuer;
    @Value("${jwt.audience:fit-a-pet-api}") private String audience;
    private SecretKey key;

    @PostConstruct public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (accessTokenExpiration < 1000 || refreshTokenExpiration < 1000)
            throw new IllegalArgumentException("Token lifetimes must be at least one second");
    }
    public long newSessionExpiration() {
        return (System.currentTimeMillis() + refreshTokenExpiration) / 1000 * 1000;
    }
    public TokenDto createToken(Member member, String sid, long sessionExpiration) {
        long now = System.currentTimeMillis();
        return new TokenDto(issue(member.getEmail(), sid, "access", Math.min(now + accessTokenExpiration, sessionExpiration)),
                issue(member.getEmail(), sid, "refresh", sessionExpiration));
    }
    private String issue(String email, String sid, String use, long expiration) {
        return Jwts.builder().subject(email).issuer(issuer).audience().add(audience).and()
                .id(UUID.randomUUID().toString()).claim("sid", sid).claim("token_use", use)
                .issuedAt(new Date()).expiration(new Date(expiration)).signWith(key).compact();
    }
    public Claims parse(String token, String use) {
        Claims c = Jwts.parser().verifyWith(key).requireIssuer(issuer).requireAudience(audience)
                .build().parseSignedClaims(token).getPayload();
        return validateClaims(c, use);
    }
    public Claims parseForLogout(String token) {
        try { return parse(token, "access"); }
        catch (ExpiredJwtException e) { return validateClaims(e.getClaims(), "access"); }
    }
    private Claims validateClaims(Claims c, String use) {
        if (!issuer.equals(c.getIssuer()) || c.getAudience() == null || !c.getAudience().contains(audience))
            throw new JwtException("Invalid token recipient");
        if (!use.equals(c.get("token_use", String.class)) || c.getSubject() == null || c.getSubject().isBlank()
                || c.getId() == null || c.getExpiration() == null || c.getIssuedAt() == null)
            throw new JwtException("Invalid token claims");
        try { UUID.fromString(c.get("sid", String.class)); UUID.fromString(c.getId()); }
        catch (RuntimeException e) { throw new JwtException("Invalid token identity"); }
        return c;
    }
    public boolean validateAccessToken(String token) {
        try { parse(token, "access"); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
    public long getExpirationTime(String token) {
        return parseForLogout(token).getExpiration().getTime() - System.currentTimeMillis();
    }
}
