package com.osigie.payment_gateway.dto.payment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CardDetailsDto(
        @NotBlank(message = "Card number is required")
        String cardNumber,
        @NotBlank(message = "CVV is required")
        String cvv,
        @Min(value = 1, message = "Expiry month must be between 1 and 12")
        @Max(value = 12, message = "Expiry month must be between 1 and 12")
        int expiryMonth,
        @Min(value = 2024, message = "Expiry year must be 2024 or later")
        int expiryYear) {
}
