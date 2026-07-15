package com.osigie.payment_gateway.service.recovery.handler;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

public interface RecoveryHandler {

    Operation operation();

    void resume(IdempotencyKey key);
}
