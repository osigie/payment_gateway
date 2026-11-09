package com.osigie.payment_gateway.dto.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAuthorizationRequestDto(
    @NotBlank(message = "Merchant order ID is required") String merchantOrderId,
    @NotBlank(message = "Merchant customer ID is required") String merchantCustomerId,
    @Min(value = 10, message = "Amount must be at least 10") long amountMinor,
    @NotNull(message = "Card details are required") @Valid CardDetailsDto cardDetails) {}
