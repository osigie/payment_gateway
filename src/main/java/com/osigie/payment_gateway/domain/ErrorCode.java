package com.osigie.payment_gateway.domain;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    BANK_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
