package com.osigie.payment_gateway.domain;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

public record CaptureContext(String authorizationRefId, long amount, IdempotencyKey idempotencyKey) {
}
