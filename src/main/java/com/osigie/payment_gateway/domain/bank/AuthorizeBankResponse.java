package com.osigie.payment_gateway.domain.bank;

import java.time.OffsetDateTime;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AuthorizeBankResponse(
    long amount,
    String authorizationId,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    String status) {}
