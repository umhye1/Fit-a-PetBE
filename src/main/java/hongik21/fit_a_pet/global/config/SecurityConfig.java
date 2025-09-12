package hongik21.fit_a_pet.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hongik21.fit_a_pet.global.jwt.CustomAccessDeniedHandler;
import hongik21.fit_a_pet.global.jwt.CustomAuthenticationEntryPoint;
//import hongik21.fit_a_pet.global.jwt.JwtFilter;
import hongik21.fit_a_pet.global.jwt.JwtProvider;
import hongik21.fit_a_pet.global.jwt.RedisJwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RedisJwtFilter jwtFilter;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors ->
                cors.configurationSource(corsConfigurationSource())
        );

        http.csrf(AbstractHttpConfigurer::disable);

        http.logout(AbstractHttpConfigurer::disable);

        http.formLogin(AbstractHttpConfigurer::disable);

        // 세션 미사용
        http.sessionManagement((session) ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // before filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // exception handler
        http.exceptionHandling(conf -> conf
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );

        // 요청 URI 권한 설정
        http.authorizeHttpRequests((authorize)->
                authorize
                        .requestMatchers("/", "/error/**").permitAll()
                        // 로그인 및 가입 로직
                        .requestMatchers("/api/users/login/**").permitAll()
                        .requestMatchers("/api/users/signup/**").permitAll()
                        .requestMatchers("/api/users/email/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/image/upload").permitAll()
                        .requestMatchers("/api/users/logout").authenticated()
                        .requestMatchers("/api/mypage/**").authenticated()
                        .anyRequest().authenticated()
        );

        return http.build();
    }

    /**
     * CORS 허용 커스터마이징
     * @return 변경된 CORS 정책
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 인증정보 주고받도록 허용
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000")); // 기본
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;

    }
}
