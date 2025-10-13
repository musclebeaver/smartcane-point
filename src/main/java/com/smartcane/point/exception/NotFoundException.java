package com.smartcane.point.exception;

import org.springframework.http.HttpStatus;

/**
 * 리소스(엔티티) 미발견용 예외.
 * 기본 상태코드는 404 NOT_FOUND.
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String resourceName) {
        super(resourceName != null ? resourceName + "_NOT_FOUND" : "NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
