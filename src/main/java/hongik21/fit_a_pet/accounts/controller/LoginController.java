package hongik21.fit_a_pet.accounts.controller;

import hongik21.fit_a_pet.accounts.dto.*;
import hongik21.fit_a_pet.accounts.service.*;
import hongik21.fit_a_pet.global.CommonResponse;
import hongik21.fit_a_pet.global.jwt.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class LoginController {
    private final JwtProvider jwt;
    private final TokenService tokens;
    private final MemberService members;
    public record RefreshRequest(@NotBlank String refreshToken) {}
    @PostMapping("/api/users/login")
    public CommonResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return CommonResponse.onSuccess(members.login(request), "로그인을 성공했습니다.");
    }
    @PostMapping("/api/auth/refresh")
    public CommonResponse<TokenDto> refresh(@RequestBody @Valid RefreshRequest request) {
        return CommonResponse.onSuccess(tokens.refresh(request.refreshToken()), "토큰을 재발급했습니다.");
    }
    @PostMapping("/api/users/logout")
    public CommonResponse<Void> logout(HttpServletRequest request) {
        tokens.logout(jwt.resolveToken(request));
        return CommonResponse.onSuccess(null, "로그아웃을 성공했습니다.");
    }
}
