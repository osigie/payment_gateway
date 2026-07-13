package com.osigie.payment_gateway.dto.payment;

import com.osigie.payment_gateway.domain.PaymentStatus;
import com.osigie.payment_gateway.domain.entity.Payment;

import java.time.OffsetDateTime;

public record PaymentResponse(
        String merchantOrderId,
        String merchantCustomerId,
        long amount_minor,
        PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PaymentResponse to(Payment payment) {
        return new PaymentResponse(payment.getMerchantOrderId(), payment.getMerchantCustomerId(), payment.getAmountMinor(), payment.getStatus(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

}
