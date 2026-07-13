package com.osigie.payment_gateway.domain;

public enum IdempotencyKeyType {
    AUTHORIZED, CAPTURE, REFUND, VOID
}
