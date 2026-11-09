package com.osigie.payment_gateway.config.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.osigie.payment_gateway.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyInterceptorTests {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private final IdempotencyKeyInterceptor interceptor = new IdempotencyKeyInterceptor();

  @Test
  void preHandle_withPostRequestAndValidKey_shouldReturnTrue() {
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader("x-idempotency-key")).thenReturn("valid-key");

    var result = interceptor.preHandle(request, response, new Object());

    assertThat(result).isTrue();
  }

  @Test
  void preHandle_withPostRequestAndNullKey_shouldThrowBadRequestException() {
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader("x-idempotency-key")).thenReturn(null);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("x-idempotency-key is null or blank");
  }

  @Test
  void preHandle_withPostRequestAndBlankKey_shouldThrowBadRequestException() {
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader("x-idempotency-key")).thenReturn("   ");

    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("x-idempotency-key is null or blank");
  }

  @Test
  void preHandle_withNonPostRequest_shouldReturnTrue() {
    when(request.getMethod()).thenReturn("GET");

    var result = interceptor.preHandle(request, response, new Object());

    assertThat(result).isTrue();
  }
}
