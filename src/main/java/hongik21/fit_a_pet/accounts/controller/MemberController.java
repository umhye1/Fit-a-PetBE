package hongik21.fit_a_pet.accounts.controller;

import hongik21.fit_a_pet.accounts.dto.*;
import hongik21.fit_a_pet.accounts.service.MemberService;
import hongik21.fit_a_pet.global.CommonResponse;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/email")
    public CommonResponse<Object> sendVerifyMail(@RequestBody MailRequest request) throws Exception {

        memberService.sendCodeToEmail(request.getEmail());
        return CommonResponse.onSuccess(null, "인증 메일을 전송했습니다.");
    }

    @PostMapping("/email/verify")
    public CommonResponse<Object> verifyMailCode(@RequestBody MailVerifyRequest request) throws ApplicationException {

        memberService.verifyMailCode(request.getEmail(), request.getCode());
        return CommonResponse.onSuccess(null, "이메일 인증을 성공했습니다.");
    }

    @PostMapping("/signup")
    public CommonResponse<JoinResponse> signup(@RequestBody @Valid JoinRequest request) throws ApplicationException {
        JoinResponse response = memberService.join(request);
        return CommonResponse.onSuccess(response,"회원가입을 성공했습니다.");
    }

//    @PostMapping("/login")
//    public CommonResponse<Object> login(@RequestBody @Valid LoginRequest request) throws ApplicationException {
//        LoginResponse response = memberService.login(request);
//        return CommonResponse.onSuccess(response, "로그인을 성공했습니다.");
//    }
//
//    @PostMapping("/logout")
//    public CommonResponse<Object> logout() {
//        // JWT 필터에서 이미 인증했으므로 SecurityContext에서 사용자 정보 가져오기
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String email = auth.getName(); // 또는 사용자 ID
//
//        memberService.logout(email);
//        return CommonResponse.onSuccess(null, "로그아웃을 성공했습니다.");
//    }

    // 로그인 api 검증용 함수
    @GetMapping("/test")
    public String loginTest() {
        return "login user";
    }
}
