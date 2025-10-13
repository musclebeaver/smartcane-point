package com.smartcane.point.exception;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 규칙 위반을 표현하는 런타임 예외.
 * - message에는 "에러코드"를 넣어두면 GlobalExceptionHandler에서 그대로 code로 사용합니다.
 * - status는 기본 BAD_REQUEST이며, 필요 시 다른 상태코드로 지정 가능합니다.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String code) {
        super(code);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, HttpStatus status) {
        super(code);
        this.status = status == null ? HttpStatus.BAD_REQUEST : status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
