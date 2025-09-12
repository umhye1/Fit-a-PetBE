//package hongik21.fit_a_pet.global.jwt;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
///**
// * 로그인 시 인증(인가)을 확인하는 필터
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtFilter extends OncePerRequestFilter {
//
//    private final JwtProvider jwtProvider;
//    private final CustomMemberDetailsService memberDetailsService;
//
//    // 헤더 검사, 토큰에서 사용자 정보 파싱, 로그인 여부 확인, 로그 남기기, 악성 요청 차단 등의 일을 추가할 수 있음
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String token = jwtProvider.resolveToken(request);
//
//        // 토큰 검증 후 SecurityContextHolder 내 인증 정보가 없는 경우 저장
//        if(token !=null && jwtProvider.validateAccessToken(token)
//                && SecurityContextHolder.getContext().getAuthentication() == null) {
//            SecurityContextHolder.getContext().setAuthentication(getAuthentication(token));
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    // 토큰의 이메일 정보를 통해 사용자 정보를 조회하고 User~ 객체 생성
//    private UsernamePasswordAuthenticationToken getAuthentication(String token) {
//        UserDetails userDetails = memberDetailsService.loadUserByUsername(JwtProvider.getEmail(token));
//        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//    }
//
//}
