package hongik21.fit_a_pet.global.jwt;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;


/// TODO : 토큰 재발급 함수

@Slf4j
@Component
public class JwtProvider {
    private final MemberRepository memberRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static SecretKey key;

    public JwtProvider(MemberRepository memberRepository, MemberRepository memberRepository1) {
        this.memberRepository = memberRepository1;
    }

    @PostConstruct
    public void init() {

        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 액세스 토큰 생성하는 함수
     * @param member 멤버 정보 추출해서 토큰 생성
     * @return 생성된 토큰
     */
    public String createAccessToken(Member member) {
        Date now = new Date();

        return Jwts.builder()
                .setClaims(getClaims(member))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }


    public String createRefreshToken(Member member) {
        Date now = new Date();

        return Jwts.builder()
                .setClaims(getClaims(member))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpiration))
                .signWith(key)
                .compact();
    }

    /**
     * 로그인 시에 액세스 토큰과 리프레시 토큰을 생성하는 함수
     * @param member 로그인 사용자
     * @return 토큰들 정보 반환
     */
    public TokenDto createToken(Member member) {
        return TokenDto.builder()
                .accessToken(createAccessToken(member))
                .refreshToken(createRefreshToken(member))
                .build();
    }

    // 토큰 추출 함수
    public String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        // Token 정보 존재 여부 및 Bearer 토큰인지 확인
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        return null;
    }

    /**
     * 토큰 유효성 검증 함수
     * @param token 검증할 토큰
     * @return 유효 여부
     */
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다. {} ",e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다. {} ",e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다. {} ",e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다. {} ",e.getMessage());
        }
        return false;
    }

    /**
     * Claim 정보 생성해서 반환까지 해주는 함수
     * @param member 사용자를 구분할 수 있는 이메일 정보와 역할 활용
     * @return 사용자 구분 정보를 저장한 Claim 객체 반환
     */
    private Claims getClaims(Member member) {
        return Jwts.claims()
                .subject(member.getEmail())
                .add("role", member.getMemberType())
                .build();
    }

    /**
     * 토큰에서 사용자 이메일을 가져옵니다.
     * @param token
     * @return
     */
    public static String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

     /**
     * 토큰에서 사용자 ID를 가져옵니다.
     * @param token
     * @return
     */
    public Long getMemberIdFromToken(String token) {
        String email = getEmail(token);
        Optional<Member> member = memberRepository.findByEmail(email);
        return member.map(Member::getId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 토큰의 남은 유효 시간을 밀리초 단위로 가져옵니다.
     * @param token
     * @return
     */
    public long getExpirationTime(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiration.getTime() - new Date().getTime();
    }

    public String reissueToken() {
        return "";
    }


}
