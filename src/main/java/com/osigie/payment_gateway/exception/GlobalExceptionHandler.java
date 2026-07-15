package com.osigie.payment_gateway.exception;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.dto.ApiError;
import com.osigie.payment_gateway.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(BaseResponse.failure(ErrorCode.RESOURCE_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(BaseResponse.failure(ErrorCode.BAD_REQUEST.name(), ex.getMessage()));
    }


    @ExceptionHandler(BankUnavailableException.class)
    public ResponseEntity<BaseResponse<Void>> handleBankUnavailable(BankUnavailableException ex) {
        log.warn("Bank Unavailable: {}", ex.getMessage());

        return ResponseEntity
                .status(ErrorCode.BANK_UNAVAILABLE.getHttpStatus())
                .body(BaseResponse.failure(ErrorCode.BANK_UNAVAILABLE.name(), ex.getMessage()));
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.AUTHORIZATION_REQUIRED.getHttpStatus())
                .body(BaseResponse.failure(ErrorCode.AUTHORIZATION_REQUIRED.name(), "Authentication required"));
    }


    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Invalid api key: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_API_KEY.getHttpStatus())
                .body(BaseResponse.failure(ErrorCode.INVALID_API_KEY.name(), "Authentication required"));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(
                        error -> {
                            String fieldName = ((FieldError) error).getField();
                            String errorMessage = error.getDefaultMessage();
                            errors.put(fieldName, errorMessage);
                        });

        log.warn("Validation errors: {}", errors);

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(new BaseResponse<>(false, errors,
                        new ApiError(ErrorCode.VALIDATION_FAILED.name(), "Validation failed")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.failure("INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred"));
    }


}
