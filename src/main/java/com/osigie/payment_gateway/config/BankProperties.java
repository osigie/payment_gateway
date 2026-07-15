package com.osigie.payment_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "server.bank")
public record BankProperties(String baseUrl, Duration timeout, Duration readTimeout) {
}
