package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.PhaseResult;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.bank.recovery_points.AuthorizeRecoveryPoints;
import com.osigie.payment_gateway.service.AtomicPhaseExecutor;
import com.osigie.payment_gateway.service.IdempotencyKeyService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.function.Function;

@Service
public class AtomicPhaseExecutorImpl implements AtomicPhaseExecutor {
    private final ObjectMapper objectMapper;
    private final IdempotencyKeyService idempotencyKeyService;

    public AtomicPhaseExecutorImpl(ObjectMapper objectMapper, IdempotencyKeyService idempotencyKeyService) {
        this.objectMapper = objectMapper;
        this.idempotencyKeyService = idempotencyKeyService;
    }

    @Transactional
    @Override
    public void execute(UUID merchantId, String idempotencyKey, String requestPath, Function<IdempotencyKey, PhaseResult> phase) {
        execute(merchantId, idempotencyKey,requestPath, Function.identity(), phase);
    }


    @Transactional
    @Override
    public <T> void execute(UUID merchantId, String idempotencyKey, String requestPath,
                            Function<IdempotencyKey, T> loader,
                            Function<T, PhaseResult> phase) {

        IdempotencyKey key = idempotencyKeyService
                .findIdempotencyForUpdate(idempotencyKey, merchantId, requestPath)
                .orElseThrow(() -> new IllegalStateException("Idempotency key not found"));


        T resource = loader.apply(key);

        PhaseResult phaseResult = phase.apply(resource);

        switch (phaseResult) {
            case PhaseResult.NoOp ignored -> {
            }
            case PhaseResult.RecoveryPoint recoveryPoint -> {
                key.setRecoveryPoint(recoveryPoint.name());
            }
            case PhaseResult.Response<?> response -> {
                key.setRecoveryPoint(AuthorizeRecoveryPoints.FINISHED);
                int status = 200;
                if (response.result().error() != null) {
                    status = response.result().error().code().getHttpStatus().value();
                }
                key.setResponseStatus(status);
                key.setResponseBody(serialize(response.result()));

            }
        }
        idempotencyKeyService.update(key);
    }

    private String serialize(Object body) {

        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
