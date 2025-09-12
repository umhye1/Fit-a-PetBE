package hongik21.fit_a_pet.global.jwt;

import hongik21.fit_a_pet.accounts.service.RedisRefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisJwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomMemberDetailsService memberDetailsService;
    private final RedisRefreshTokenService redisRefreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

    String token = jwtProvider.resolveToken(request);
    if (token != null) {
        // 토큰이 블랙리스트에 있는지 확인 (로그아웃 여부)
        if(redisRefreshTokenService.isTokenBlacklisted(token)){
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰이 로그아웃되었습니다.");
            return;
        }

        if (jwtProvider.validateAccessToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(getAuthentication(token));
        }
    }
    filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(String token) {
        UserDetails userDetails = memberDetailsService.loadUserByUsername(JwtProvider.getEmail(token));
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

}
