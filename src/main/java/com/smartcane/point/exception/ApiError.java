package com.smartcane.point.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * 에러 응답 표준 포맷.
 * - code: 애플리케이션 에러 코드(예: INSUFFICIENT_POINT, PAYMENT_NOT_FOUND 등)
 * - message: 추가 설명(개발/운영 로그용), 클라이언트에 굳이 노출하지 않으려면 빈 문자열로
 * - details: 필드별 에러 등 부가 정보
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        Map<String, Object> details
) {
    public static ApiError of(String code, String message, int status, String path, Map<String, Object> details) {
        return new ApiError(code, message, status, path, Instant.now(), details);
    }
}
