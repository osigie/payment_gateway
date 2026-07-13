package com.osigie.payment_gateway.domain.bank.context;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

public record VoidContext(String authorizationRefId, IdempotencyKey idempotencyKey) {
}
