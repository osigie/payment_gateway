package com.osigie.payment_gateway.dto.payment;

import com.osigie.payment_gateway.domain.PaymentStatus;

import java.time.OffsetDateTime;

public record PaymentResponse(
        String merchantOrderId,
        String merchantCustomerId,
        long amount_minor,
        PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

}
