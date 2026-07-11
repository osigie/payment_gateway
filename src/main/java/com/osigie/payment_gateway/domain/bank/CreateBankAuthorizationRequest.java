package com.osigie.payment_gateway.domain.bank;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateBankAuthorizationRequest(long amount, String cardNumber, String cvv, int expiryMonth,
                                             int expiryYear) {
}
