package hongik21.fit_a_pet.global.exception;

import hongik21.fit_a_pet.global.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice(annotations = RestController.class)   // 해당 어노테이션의 모든 컨트롤러 대상
public class CustomExceptionHandler {

    @ExceptionHandler({io.jsonwebtoken.JwtException.class})
    public ResponseEntity<?> invalidToken(Exception ex) {
        return ResponseEntity.status(401).body(CommonResponse.onFailure(null, CustomErrorCode.WRONG_TOKEN));
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<?> storageUnavailable(Exception ex) {
        return ResponseEntity.status(503).body(java.util.Map.of("statusCode",503,"message","Storage unavailable"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<String> errors = fieldErrors.stream()
                .map(error -> String.format("'%s': '%s'",error.getField(),error.getDefaultMessage()))
                .toList();

        CommonResponse<?> response = CommonResponse.onFailure(errors, CustomErrorCode.INVALID_REQUEST_DTO);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonResponse<Void>> handleCustomException(ApplicationException ex) {
        CommonResponse<Void> response = CommonResponse.onFailure(null, ex.getErrorCode());
        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(response);
    }
}
