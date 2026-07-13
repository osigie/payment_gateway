package com.osigie.payment_gateway.dto;



public record BaseResponse<T>(

        boolean success,
        T data,
        ApiError error
) {


    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, data, null);
    }

    public static <T> BaseResponse<T> failure(String code, String message) {
        return new BaseResponse<>(false, null, new ApiError(code, message));
    }
}
