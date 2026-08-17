package glowbook.exception;

import glowbook.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bu işlem için yetkin yok."));
    }

    /**
     * Every field-level validation message shown here comes from an explicit Turkish
     * {@code message=} on the DTO's own {@code @NotBlank}/{@code @NotNull}/etc. annotation
     * (see the DTO classes in {@code glowbook.dto}) — never the raw Java field name and
     * never Hibernate Validator's built-in English default, both of which used to leak
     * straight through to the customer on any validation failure.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message.isBlank() ? "Girdiğin bilgileri kontrol edip tekrar dene." : message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Beklenmedik bir hata oluştu. Lütfen tekrar dene."));
    }
}
