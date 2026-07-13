package com.osigie.payment_gateway.domain;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;

import java.util.UUID;

public record AuthorizationContext(
        UUID merchantId,
        IdempotencyKey idempotencyKey,
        CreateAuthorizationRequestDto dto
) {
}
