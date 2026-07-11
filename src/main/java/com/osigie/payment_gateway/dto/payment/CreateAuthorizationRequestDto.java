package com.osigie.payment_gateway.dto.payment;

public record CreateAuthorizationRequestDto(
        String merchantOrderId,
        String merchantCustomerId,
        long amountMinor,
        CardDetailsDto cardDetails
) {
}


