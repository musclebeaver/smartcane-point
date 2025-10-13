package com.smartcane.point.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 * - BusinessException, NotFoundException : 우리가 던진 비즈니스 에러
 * - MethodArgumentNotValidException     : @Valid 바디 검증 실패
 * - ConstraintViolationException        : @RequestParam/@PathVariable 검증 실패
 * - HttpMessageNotReadableException     : JSON 파싱 실패
 * - Exception                           : 알 수 없는 에러(500)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req) {
        HttpStatus status = ex.getStatus();
        String code = safeCode(ex.getMessage(), "BUSINESS_ERROR");
        log.warn("[BUSINESS] {} {} - {} ({})", req.getMethod(), req.getRequestURI(), code, status.value(), ex);
        ApiError body = ApiError.of(code, ex.getMessage(), status.value(), req.getRequestURI(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String code = safeCode(ex.getMessage(), "NOT_FOUND");
        log.warn("[NOT_FOUND] {} {} - {}", req.getMethod(), req.getRequestURI(), code);
        ApiError body = ApiError.of(code, ex.getMessage(), status.value(), req.getRequestURI(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        details.put("fieldErrors", fieldErrors);
        String code = "INVALID_REQUEST";
        log.warn("[VALIDATION] {} {} - {}", req.getMethod(), req.getRequestURI(), fieldErrors);
        ApiError body = ApiError.of(code, "Request body validation failed", status.value(), req.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> details = new HashMap<>();
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        GlobalExceptionHandler::violationPath,
                        ConstraintViolation::getMessage,
                        (a, b) -> a
                ));
        details.put("violations", violations);
        String code = "INVALID_PARAMETER";
        log.warn("[CONSTRAINT] {} {} - {}", req.getMethod(), req.getRequestURI(), violations);
        ApiError body = ApiError.of(code, "Parameter validation failed", status.value(), req.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "MALFORMED_JSON";
        log.warn("[JSON] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        ApiError body = ApiError.of(code, "Malformed JSON request", status.value(), req.getRequestURI(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "INTERNAL_ERROR";
        log.error("[UNKNOWN] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        ApiError body = ApiError.of(code, "Unexpected error", status.value(), req.getRequestURI(), null);
        return ResponseEntity.status(status).body(body);
    }

    private static String violationPath(ConstraintViolation<?> v) {
        try {
            return v.getPropertyPath().toString();
        } catch (Exception e) {
            return "param";
        }
    }

    private static String safeCode(String msg, String fallback) {
        if (msg == null || msg.isBlank()) return fallback;
        // 공백/슬래시 등은 클라이언트에서 코드 파싱 시 혼란을 줄 수 있어 간단히 정제
        return msg.trim().replace(' ', '_').toUpperCase();
    }
}
