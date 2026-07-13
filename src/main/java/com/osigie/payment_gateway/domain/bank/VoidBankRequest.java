package com.osigie.payment_gateway.domain.bank;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VoidBankRequest(String authorizationId) {
}
