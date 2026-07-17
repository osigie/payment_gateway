package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.bank.*;
import com.osigie.payment_gateway.exception.BankBusinessException;
import com.osigie.payment_gateway.exception.BankUnavailableException;
import com.osigie.payment_gateway.service.BankClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class BankClientImpl implements BankClient {
  private final RestClient bankRestClient;

  public BankClientImpl(RestClient restClient) {
    this.bankRestClient = restClient;
  }

  @Retryable(
      includes = {BankUnavailableException.class, ResourceAccessException.class},
      maxRetries = 3,
      delay = 500,
      multiplier = 2.0,
      jitter = 50)
  public AuthorizeBankResponse authorize(AuthorizeBankRequest request, String idempotencyKey) {
    return post("/authorizations", request, idempotencyKey, AuthorizeBankResponse.class);
  }

  @Retryable(
      includes = {BankUnavailableException.class, ResourceAccessException.class},
      maxRetries = 3,
      delay = 500,
      multiplier = 2.0,
      jitter = 50)
  @Override
  public CaptureBankResponse capture(CaptureBankRequest request, String idempotencyKey) {
    return post("/captures", request, idempotencyKey, CaptureBankResponse.class);
  }

  @Retryable(
      includes = {BankUnavailableException.class, ResourceAccessException.class},
      maxRetries = 3,
      delay = 500,
      multiplier = 2.0,
      jitter = 50)
  @Override
  public VoidBankResponse _void(VoidBankRequest request, String idempotencyKey) {
    return post("/void", request, idempotencyKey, VoidBankResponse.class);
  }

  @Retryable(
      includes = {BankUnavailableException.class, ResourceAccessException.class},
      maxRetries = 3,
      delay = 500,
      multiplier = 2.0,
      jitter = 50)
  @Override
  public RefundBankResponse refund(RefundBankRequest request, String idempotencyKey) {
    return post("/refunds", request, idempotencyKey, RefundBankResponse.class);
  }

  private <T> T post(String uri, Object request, String idempotencyKey, Class<T> responseType) {
    try {

      return bankRestClient
          .post()
          .uri(uri)
          .header("Idempotency-Key", idempotencyKey)
          .body(request)
          .exchange(
              (clientRequest, clientResponse) -> {
                HttpStatusCode status = clientResponse.getStatusCode();

                if (status.is2xxSuccessful()) {
                  return clientResponse.bodyTo(responseType);
                }
                BankErrorResponse error = null;
                try {

                  error = clientResponse.bodyTo(BankErrorResponse.class);
                } catch (Exception _) {
                  // in case error structure changes
                }

                if (error == null) {
                  throw new BankUnavailableException(
                      "Bank returned " + status + " with empty body");
                }

                if (status.is4xxClientError()) {
                  throw new BankBusinessException(error.message(), error.error(), status);
                }

                throw new BankUnavailableException(error.message());
              });
    } catch (ResourceAccessException ex) {
      throw new BankUnavailableException(
          "Bank is temporarily unavailable please try again later", ex);
    }
  }

  public ErrorCode mapBankErrrorToErrorCode(HttpStatusCode status) {
    return switch (status) {
      case HttpStatus.BAD_REQUEST -> ErrorCode.BAD_REQUEST;
      case HttpStatus.PAYMENT_REQUIRED -> ErrorCode.INSUFFICIENT_FUNDS;
      default -> ErrorCode.BANK_UNAVAILABLE;
    };
  }
}
