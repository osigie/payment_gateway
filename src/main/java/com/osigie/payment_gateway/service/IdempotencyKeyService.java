package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.IdempotencyKeyType;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyService {

    IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, String requestParams, String requestPath);

    IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, UUID paymentId, String requestParams, String requestPath);

    Optional<IdempotencyKey> findIdempotencyForUpdate(String idempotencyKey, UUID merchantId, String requestPath);


    void update(IdempotencyKey idempotencyKey);
}
