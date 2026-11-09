package com.osigie.payment_gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server.bank")
public record BankProperties(String baseUrl, Duration timeout, Duration readTimeout) {}
