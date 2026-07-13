package com.osigie.payment_gateway.dto.payment;

import com.osigie.payment_gateway.domain.PaymentStatus;
import com.osigie.payment_gateway.domain.entity.Payment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        String merchantOrderId,
        String merchantCustomerId,
        long amount_minor,
        PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID id,
        PaymentStatus paymentStatus

) {


}
