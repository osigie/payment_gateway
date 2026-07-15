package com.osigie.payment_gateway.domain.bank.context;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

public record RefundContext(String captureRefId, IdempotencyKey idempotencyKey) {
}
