package hongik21.fit_a_pet.global.config;

import hongik21.fit_a_pet.global.jwt.RedisJwtFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, RedisJwtFilter jwt) throws Exception {
        return http.csrf(c -> c.disable()).cors(c -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable()).httpBasic(b -> b.disable()).logout(l -> l.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint((r,s,x) -> s.sendError(401))
                        .accessDeniedHandler((r,s,x) -> s.sendError(403)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/test/**", "/posts/doTest", "/trails/doTest").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/signup", "/api/users/email",
                                "/api/users/email/verify", "/api/auth/refresh", "/api/users/logout").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean FilterRegistrationBean<RedisJwtFilter> disableServletFilter(RedisJwtFilter jwt) {
        var registration = new FilterRegistrationBean<>(jwt);
        registration.setEnabled(false);
        return registration;
    }
    @Bean CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> origins) {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type"));
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", c); return source;
    }
}
