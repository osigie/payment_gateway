package com.osigie.payment_gateway.dto;

import com.osigie.payment_gateway.domain.Error;
import com.osigie.payment_gateway.domain.ErrorCode;

import java.util.function.Function;

public record Result<T>(boolean success, T data, Error error) {

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null);
    }

    public static <T> Result<T> failure(ErrorCode code, String message) {
        return new Result<>(false, null, new Error(code, message));
    }

    public <R> Result<R> map(Function<T, R> mapper) {
        if (!success) {
            return new Result<>(false, null, error);
        }
        return Result.success(mapper.apply(data));
    }
}
