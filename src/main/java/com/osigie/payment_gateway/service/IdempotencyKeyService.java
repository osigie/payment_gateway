package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyService {

    IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, String requestParams, Operation operation);

    IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, UUID paymentId, String requestParams, Operation operation);

    Optional<IdempotencyKey> findIdempotencyForUpdate(String idempotencyKey, UUID merchantId, Operation operation);


    void update(IdempotencyKey idempotencyKey);
}
