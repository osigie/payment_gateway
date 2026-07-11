package com.osigie.payment_gateway.dto.payment;

public   record CardDetailsDto(
        String cardNumber,
        String cvv,
        int expiryMonth,
        int expiryYear) {
}
