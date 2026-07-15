package com.osigie.payment_gateway.service.recovery.handler;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.stereotype.Component;

@Component
public class CaptureRecoveryHandler implements RecoveryHandler {

    private final PaymentService paymentService;

    public CaptureRecoveryHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Operation operation() {
        return Operation.PAYMENT_CAPTURE;
    }

    @Override
    public void resume(IdempotencyKey key) {

        if (key.getPayment() == null) {
            throw new IllegalStateException(
                    "Payment not found for idempotency key " + key.getId()
            );
        }

        paymentService.createCapture(
                key.getPayment().getId(),
                key.getMerchant().getId(),
                key.getIdempotencyKey(),
                Operation.PAYMENT_CAPTURE
        );
    }
}
