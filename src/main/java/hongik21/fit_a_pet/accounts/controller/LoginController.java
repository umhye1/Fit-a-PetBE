package hongik21.fit_a_pet.accounts.controller;

import hongik21.fit_a_pet.accounts.dto.LoginRequest;
import hongik21.fit_a_pet.accounts.dto.LoginResponse;
import hongik21.fit_a_pet.accounts.service.MemberService;
import hongik21.fit_a_pet.accounts.service.RedisRefreshTokenService;
import hongik21.fit_a_pet.global.CommonResponse;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import hongik21.fit_a_pet.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class LoginController {

    private final JwtProvider jwtProvider;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final MemberService memberService;

    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) throws ApplicationException{
        LoginResponse response = memberService.login(request);

        // redis 기반 토큰 저장
        String accessToken = response.getAccessToken();
        String refreshToken = response.getRefreshToken();
        Long memberId = response.getId();

        // refresh token을 redis에 저장
        redisRefreshTokenService.save(memberId, refreshToken);

        return CommonResponse.onSuccess(response, "로그인을 성공했습니다.");
    }

    @PostMapping("/logout")
    public CommonResponse<Object> logout(HttpServletRequest request){
        String accessToken  = jwtProvider.resolveToken(request);
        if(accessToken != null){

            // refresh token 삭제
            Long memberId = jwtProvider.getMemberIdFromToken(accessToken);
            redisRefreshTokenService.delete(memberId);

            // access token 블랙리스트 추가
            redisRefreshTokenService.addToBlacklist(accessToken);

            return CommonResponse.onSuccess(null, "로그아웃을 성공했습니다.");
        }
        return CommonResponse.onFailure(null, CustomErrorCode.TOKEN_NOT_FOUND);
    }


}
