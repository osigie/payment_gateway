package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.PhaseResult;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

import java.util.UUID;
import java.util.function.Function;

public interface AtomicPhaseExecutor {
    <T> void execute(UUID merchantId, String idempotencyKey, String requestPath, Function<IdempotencyKey, T> loader,
                     Function<T, PhaseResult> phase);

    void execute(UUID merchantId, String idempotencyKey, String requestPath, Function<IdempotencyKey, PhaseResult> phase);


}
