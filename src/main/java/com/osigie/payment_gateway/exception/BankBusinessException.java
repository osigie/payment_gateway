package com.osigie.payment_gateway.exception;

import org.springframework.http.HttpStatusCode;

public class BankBusinessException extends RuntimeException {
    private final String error;
    private final HttpStatusCode status;

    public BankBusinessException(String message, String error, HttpStatusCode status) {
        super(message);
        this.error = error;
        this.status = status;
    }

    public BankBusinessException(String message, HttpStatusCode status) {
        super(message);
        this.error = null;
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
