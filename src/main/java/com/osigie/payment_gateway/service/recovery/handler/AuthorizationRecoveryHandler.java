package com.osigie.payment_gateway.service.recovery.handler;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuthorizationRecoveryHandler implements RecoveryHandler {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public AuthorizationRecoveryHandler(
            PaymentService paymentService,
            ObjectMapper objectMapper
    ) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Operation operation() {
        return Operation.PAYMENT_AUTHORIZE;
    }

    @Override
    public void resume(IdempotencyKey key) {

        CreateAuthorizationRequestDto dto =
                objectMapper.readValue(
                        key.getRequestParams(),
                        CreateAuthorizationRequestDto.class
                );

        paymentService.createAuthorize(
                dto,
                key.getMerchant().getId(),
                key.getIdempotencyKey(),
                Operation.PAYMENT_AUTHORIZE
        );
    }
}
