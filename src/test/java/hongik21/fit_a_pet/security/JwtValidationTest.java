package hongik21.fit_a_pet.security;

import hongik21.fit_a_pet.global.jwt.JwtProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtValidationTest {
    static final String SECRET="test-secret-only-64-bytes-long-for-jwt-validation-12345678901234567890";
    JwtProvider jwt;
    @BeforeEach void setup() {
        jwt=new JwtProvider();
        ReflectionTestUtils.setField(jwt,"secret",SECRET);
        ReflectionTestUtils.setField(jwt,"issuer","fit-a-pet");
        ReflectionTestUtils.setField(jwt,"audience","fit-a-pet-api");
        ReflectionTestUtils.setField(jwt,"accessTokenExpiration",60000L);
        ReflectionTestUtils.setField(jwt,"refreshTokenExpiration",300000L);jwt.init();
    }
    String issue(String issuer,String audience,String use,long expires,String secret) {
        return Jwts.builder().subject("test@example.com").id(UUID.randomUUID().toString())
                .issuer(issuer).audience().add(audience).and().claim("sid",UUID.randomUUID().toString())
                .claim("token_use",use).issuedAt(new Date(System.currentTimeMillis()-10000)).expiration(new Date(expires))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    @Test void invalidSignatureIssuerAudienceAndPurposeAreRejected() {
        long expires=System.currentTimeMillis()+60000;
        for(String token:List.of(issue("other","fit-a-pet-api","access",expires,SECRET),
                issue("fit-a-pet","other","access",expires,SECRET),
                issue("fit-a-pet","fit-a-pet-api","refresh",expires,SECRET),
                issue("fit-a-pet","fit-a-pet-api","access",expires,SECRET+"different"),"malformed"))
            assertFalse(jwt.validateAccessToken(token));
    }
    @Test void expiredAccessIsOnlyAcceptedForLogoutAndKeepsNegativeTtl() {
        String expired=issue("fit-a-pet","fit-a-pet-api","access",System.currentTimeMillis()-2000,SECRET);
        assertFalse(jwt.validateAccessToken(expired));assertTrue(jwt.getExpirationTime(expired)<0);
        assertEquals("test@example.com",jwt.parseForLogout(expired).getSubject());
        String wrongIssuer=issue("other","fit-a-pet-api","access",System.currentTimeMillis()-2000,SECRET);
        assertThrows(JwtException.class,()->jwt.parseForLogout(wrongIssuer));
        String refresh=issue("fit-a-pet","fit-a-pet-api","refresh",System.currentTimeMillis()-2000,SECRET);
        assertThrows(JwtException.class,()->jwt.parseForLogout(refresh));
    }
}
