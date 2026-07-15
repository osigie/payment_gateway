package com.osigie.payment_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Collections;

@Configuration
public class RestClientConfig {
    private final BankProperties bankProperties;

    public RestClientConfig(BankProperties bankProperties) {
        this.bankProperties = bankProperties;
    }

    @Bean
    RestClient bankRestClient() {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(bankProperties.timeout())
                .build();


        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(bankProperties.readTimeout());

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                })
                .baseUrl(bankProperties.baseUrl())
                .build();
    }

}
