package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.bank.*;
import com.osigie.payment_gateway.exception.BankBusinessException;
import com.osigie.payment_gateway.exception.BankUnavailableException;
import com.osigie.payment_gateway.service.BankClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class BankClientImpl implements BankClient {


    private final RestClient restClient;

    public BankClientImpl() {
        //TODO: review this timing, needs to be small because of db transaction

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();


        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                //                TODO: take to ppties
                .baseUrl("http://localhost:8787/api/v1")
                .build();
    }


    @Retryable(
            includes = {BankUnavailableException.class, ResourceAccessException.class},
            maxRetries = 3,
            delay = 500,
            multiplier = 2.0,
            jitter = 50

    )
    public AuthorizeBankResponse authorize(AuthorizeBankRequest request, String idempotencyKey) {
        return restClient.post()
                .uri("/authorizations")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange(((clientRequest, clientResponse) -> {
                    HttpStatusCode status = clientResponse.getStatusCode();

                    if (status.is2xxSuccessful()) {
                        return clientResponse.bodyTo(AuthorizeBankResponse.class);
                    }

                    BankErrorResponse errorResponse = clientResponse.bodyTo(BankErrorResponse.class);

                    if (errorResponse == null) {
                        throw new BankUnavailableException("Bank returned " + status + " with an empty response body.");
                    }

                    if (status.is4xxClientError()) {
                        throw new BankBusinessException(errorResponse.message(), errorResponse.error(), status);
                    }

                    throw new BankUnavailableException(errorResponse.message());
                }));
    }


    @Retryable(
            includes = {BankUnavailableException.class, ResourceAccessException.class},
            maxRetries = 3,
            delay = 500,
            multiplier = 2.0,
            jitter = 50

    )
    @Override
    public CaptureBankResponse capture(CaptureBankRequest request, String idempotencyKey) {
        return restClient.post()
                .uri("/captures")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange(((clientRequest, clientResponse) -> {
                    HttpStatusCode status = clientResponse.getStatusCode();

                    if (status.is2xxSuccessful()) {
                        return clientResponse.bodyTo(CaptureBankResponse.class);
                    }

                    BankErrorResponse errorResponse = clientResponse.bodyTo(BankErrorResponse.class);

                    if (errorResponse == null) {
                        throw new BankUnavailableException("Bank returned " + status + " with an empty response body.");
                    }

                    if (status.is4xxClientError()) {
                        throw new BankBusinessException(errorResponse.message(), errorResponse.error(), status);
                    }

                    throw new BankUnavailableException(errorResponse.message());
                }));
    }

    @Retryable(
            includes = {BankUnavailableException.class, ResourceAccessException.class},
            maxRetries = 3,
            delay = 500,
            multiplier = 2.0,
            jitter = 50

    )
    @Override
    public VoidBankResponse _void(VoidBankRequest request, String idempotencyKey) {
        return restClient.post()
                .uri("/voids")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange(((clientRequest, clientResponse) -> {
                    HttpStatusCode status = clientResponse.getStatusCode();

                    if (status.is2xxSuccessful()) {
                        return clientResponse.bodyTo(VoidBankResponse.class);
                    }

                    BankErrorResponse errorResponse = clientResponse.bodyTo(BankErrorResponse.class);

                    if (errorResponse == null) {
                        throw new BankUnavailableException("Bank returned " + status + " with an empty response body.");
                    }

                    if (status.is4xxClientError()) {
                        throw new BankBusinessException(errorResponse.message(), errorResponse.error(), status);
                    }

                    throw new BankUnavailableException(errorResponse.message());
                }));
    }

    @Override
    public RefundBankResponse refund(RefundBankRequest request, String idempotencyKey) {
        return restClient.post()
                .uri("/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange(((clientRequest, clientResponse) -> {
                    HttpStatusCode status = clientResponse.getStatusCode();

                    if (status.is2xxSuccessful()) {
                        return clientResponse.bodyTo(RefundBankResponse.class);
                    }

                    BankErrorResponse errorResponse = clientResponse.bodyTo(BankErrorResponse.class);

                    if (errorResponse == null) {
                        throw new BankUnavailableException("Bank returned " + status + " with an empty response body.");
                    }

                    if (status.is4xxClientError()) {
                        throw new BankBusinessException(errorResponse.message(), errorResponse.error(), status);
                    }

                    throw new BankUnavailableException(errorResponse.message());
                }));
    }

    public ErrorCode mapBankErrrorToErrorCode(HttpStatusCode status) {
        return switch (status) {
            case HttpStatus.BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            case HttpStatus.PAYMENT_REQUIRED -> ErrorCode.INSUFFICIENT_FUNDS;
            default -> ErrorCode.BANK_UNAVAILABLE;
        };
    }
}

