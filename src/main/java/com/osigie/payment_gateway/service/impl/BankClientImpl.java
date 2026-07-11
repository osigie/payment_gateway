package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.bank.BankErrorResponse;
import com.osigie.payment_gateway.domain.bank.CreateBankAuthorizationRequest;
import com.osigie.payment_gateway.domain.bank.CreateBankAuthorizationResponse;
import com.osigie.payment_gateway.service.BankClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
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

    public CreateBankAuthorizationResponse createAuthorization(CreateBankAuthorizationRequest request, String idempotencyKey) {
        return restClient.post()
                .uri("/authorizations")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange(((clientRequest, clientResponse) -> {
                    HttpStatusCode status = clientResponse.getStatusCode();

                    if (status.is2xxSuccessful()) {
                        return clientResponse.bodyTo(CreateBankAuthorizationResponse.class);
                    }
                    BankErrorResponse errorResponse = clientResponse.bodyTo(BankErrorResponse.class);

//                    TODO: create exception for different business, and server error
                    throw new RuntimeException(errorResponse.message());
                }));
    }
}
