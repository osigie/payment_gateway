package com.osigie.payment_gateway.dto.payment;

import com.osigie.payment_gateway.domain.PaymentStatus;
import com.osigie.payment_gateway.domain.entity.Payment;

import java.time.OffsetDateTime;

public record PaymentResponseDto(
        String merchantOrderId,
        String merchantCustomerId,
        long amount_minor, PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PaymentResponseDto toDto(Payment payment) {
        return new PaymentResponseDto(payment.getMerchantOrderId(), payment.getMerchantCustomerId(), payment.getAmount_minor(), payment.getStatus(), payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
