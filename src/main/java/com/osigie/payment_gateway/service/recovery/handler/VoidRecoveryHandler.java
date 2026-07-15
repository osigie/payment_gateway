package com.osigie.payment_gateway.service.recovery.handler;


import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.stereotype.Component;

@Component
public class VoidRecoveryHandler implements RecoveryHandler {

    private final PaymentService paymentService;

    public VoidRecoveryHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Operation operation() {
        return Operation.PAYMENT_VOID;
    }

    @Override
    public void resume(IdempotencyKey key) {

        paymentService.createVoid(
                key.getPayment().getId(),
                key.getMerchant().getId(),
                key.getIdempotencyKey(),
                Operation.PAYMENT_VOID
        );
    }
}
