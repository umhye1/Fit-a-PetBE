package hongik21.fit_a_pet.accounts.service;

import hongik21.fit_a_pet.accounts.dto.JoinRequest;
import hongik21.fit_a_pet.accounts.dto.JoinResponse;
import hongik21.fit_a_pet.accounts.dto.LoginRequest;
import hongik21.fit_a_pet.accounts.dto.LoginResponse;
import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.entity.MemberType;
import hongik21.fit_a_pet.accounts.entity.VerificationCode;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.accounts.repository.VerificationCodeRepository;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import hongik21.fit_a_pet.global.jwt.JwtProvider;
import hongik21.fit_a_pet.global.jwt.TokenDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final JavaMailSender mailSender;
    private final VerificationCodeRepository verificationCodeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MemberRepository memberRepository;
//    private final RefreshTokenService refreshTokenService;


    /**
     * 이메일로 인증 코드를 보내는 함수
     * @param to 메일을 보낼 주소 (받는 사람)
     * @throws MessagingException
     */
    public void sendCodeToEmail(String to) throws Exception {
        String randomCode = createVerificationCode(to);
        String title = "Fit-A-Pet 회원가입을 위한 이메일 인증 번호";
        String content = "<html>"
                + "<body>"
                + "<h1>Fit-A-Pet 인증 코드: " + randomCode + "</h1>"
                + "<p>해당 코드를 홈페이지에 입력하세요.</p>"
                + "<footer style='color:grey; font-size: small;'>"
                + "<p>본 메일은 자동응답 메일이므로 본 메일에 회신하지 마시기 바랍니다.</p>"
                + "</footer>"
                + "</body>"
                + "</html>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(title);
        helper.setText(content, true);

        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Unable to send email in sendCodeToEmail",e); // 함수명을 포함해서 적음
        }
    }

    // 인증 코드 생성 및 저장
    public String createVerificationCode(String email) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUWVYXZ0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < 6; i++) {
            codeBuilder.append(chars.charAt(random.nextInt(chars.length())));
        }

        String randomCode = codeBuilder.toString();

        VerificationCode code = VerificationCode.builder()
                .email(email)
                .code(randomCode)
                .expiryTime(LocalDateTime.now().plusDays(1))    // 24시간 뒤 만료
                .build();

        verificationCodeRepository.save(code);
        return code.getCode();
    }


    // 이메일로 보냈던 코드와 인증한 코드가 일치하는지 확인하는 함수
    public void verifyMailCode(String email, String code) throws ApplicationException{
        verificationCodeRepository.findByEmailAndCode(email, code)
                .map(vc -> vc.getExpiryTime().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.EMAIL_VERIFY_FAILED));
    }

    // 회원가입 로직
    public JoinResponse join(JoinRequest request) throws ApplicationException {

        // 인증된 이메일인지 체크
        verificationCodeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.EMAIL_NOT_VERIFIED));

        // 이전에 가입했던 이메일인지 체크
        memberRepository.findByEmail(request.getEmail())
                .ifPresent(member -> {
                    throw new ApplicationException(CustomErrorCode.EMAIL_EXISTED);
                });

        String encodePassword = passwordEncoder.encode(request.getPassword());

        Member member = Member.builder()
                .name(request.getName())
                .email(request.getEmail())
                .nickname(request.getNickname())
                .password(encodePassword)
                .memberType(MemberType.valueOf(MemberType.GENERAL.toString()))
                .createdAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        return JoinResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .nickname(member.getNickname())
                .createdAt(member.getCreatedAt())
                .build();
    }

    public LoginResponse login(LoginRequest request) throws ApplicationException {

        // 가입했는지 확인
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.MEMBER_NOT_FOUND));

        // 비밀번호 올바른지 확인
        boolean matches = passwordEncoder.matches(request.getPassword(), member.getPassword());
        if(!matches) {
            throw new ApplicationException(CustomErrorCode.PASSWORD_NOT_MATCHED);
        }

        TokenDto tokens = tokenService.login(member);

        return LoginResponse.builder()
                .id(member.getId())
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .build();

    }

    public Member getMemberByEmail(String email) throws ApplicationException {
        return memberRepository.findByEmail(email).orElseThrow(()->
                new ApplicationException(CustomErrorCode.MEMBER_NOT_FOUND));
    }
}
