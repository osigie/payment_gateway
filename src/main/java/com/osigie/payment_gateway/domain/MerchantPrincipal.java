package com.osigie.payment_gateway.domain;

import java.util.UUID;

public record MerchantPrincipal(
        UUID merchantId,
        String name
) {
}
