package hongik21.fit_a_pet.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomErrorCode {

    // 9000: 일반 에러
    ACCESS_DENIED(HttpStatus.UNAUTHORIZED, 9000, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 9001, "금지된 요청입니다."),

    // 1000: Global Error
    INVALID_REQUEST_DTO(HttpStatus.BAD_REQUEST, 1000, "요청 데이터가 조건에 만족하지 않습니다."),

    // 2000: Token Error
    WRONG_TOKEN(HttpStatus.UNAUTHORIZED, 2000, "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, 2001, "토큰이 존재하지 않습니다."),

    // 3000: Member Error
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, 3000, "유저를 찾지 못했습니다."),
    EMAIL_VERIFY_FAILED(HttpStatus.BAD_REQUEST, 3001, "인증번호가 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, 3002, "인증되지 않은 이메일로 회원가입을 시도했습니다."),
    EMAIL_EXISTED(HttpStatus.BAD_REQUEST, 3003, "이미 가입된 이메일입니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.NOT_FOUND, 3004, "비밀번호가 올바르지 않습니다."),

    // 4000: Trail Error
    TRAIL_CREATE_FAILED(HttpStatus.BAD_REQUEST, 4000, "산책 기록 생성에 실패했습니다."),
    TRAIL_NOT_FOUND(HttpStatus.BAD_REQUEST,4001, "산책 기록을 찾을 수 없습니다."),

    // 5000 : Post Error
    POST_WRITE_FAILED(HttpStatus.BAD_REQUEST, 5000, "포스트 작성에 실패했습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, 5001, "포스트를 찾을 수 없습니다."),
    INVALID_POST_CATEGORY(HttpStatus.NOT_FOUND, 5002, "포스트 카테고리를 찾을 수 없습니다."),
    // 6000 : PetPost Error
    PET_POST_WRITE_FAILED(HttpStatus.BAD_REQUEST, 6000, "펫포스트 작성에 실패했습니다."),
    PET_POST_NOT_FOUND(HttpStatus.NOT_FOUND, 6001, "펫포스트를 찾을 수 없습니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, 6002, "펫을 찾을 수 없습니다.")
    ;

    private final HttpStatus status;
    private final Integer code;
    private final String message;
}
