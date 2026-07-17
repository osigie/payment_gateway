package com.osigie.payment_gateway.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseMapper {

  private ResponseMapper() {}

  public static <T> ResponseEntity<BaseResponse<T>> toResponse(Result<T> result) {
    return toResponse(result, HttpStatus.OK);
  }

  public static <T> ResponseEntity<BaseResponse<T>> toResponse(
      Result<T> result, HttpStatus httpStatus) {
    if (result.success()) {
      return ResponseEntity.status(httpStatus).body(BaseResponse.success(result.data()));
    }

    return ResponseEntity.status(result.error().code().getHttpStatus())
        .body(BaseResponse.failure(result.error().code().name(), result.error().message()));
  }
}
