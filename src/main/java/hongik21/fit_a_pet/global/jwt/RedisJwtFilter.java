package hongik21.fit_a_pet.global.jwt;

import hongik21.fit_a_pet.accounts.repository.RedisSessionStore;
import hongik21.fit_a_pet.accounts.service.RedisRefreshTokenService;
import io.jsonwebtoken.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisJwtFilter extends OncePerRequestFilter {
    private final JwtProvider jwt;
    private final CustomMemberDetailsService members;
    private final RedisRefreshTokenService blacklist;
    private final RedisSessionStore sessions;
    private static final Set<String> PUBLIC_POSTS = Set.of("/api/users/login", "/api/users/signup",
            "/api/users/email", "/api/users/email/verify", "/api/auth/refresh", "/api/users/logout");
    @Override protected boolean shouldNotFilter(HttpServletRequest r) {
        return "POST".equals(r.getMethod()) && PUBLIC_POSTS.contains(r.getServletPath());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = jwt.resolveToken(request);
        if (token != null) {
            try {
                Claims c = jwt.parse(token, "access");
                if (!sessions.active(c.get("sid", String.class)) || blacklist.isTokenBlacklisted(token))
                    throw new JwtException("Inactive session");
                var user = members.loadUserByUsername(c.getSubject());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                response.sendError(401, "Invalid credentials"); return;
            } catch (DataAccessException e) {
                SecurityContextHolder.clearContext();
                response.sendError(503, "Authentication storage unavailable"); return;
            }
        }
        chain.doFilter(request, response);
    }
}
