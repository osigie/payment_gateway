package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyService {

    IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, String requestParams, String requestPath);

    Optional<IdempotencyKey> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    Optional<IdempotencyKey> findIdempotencyForUpdate(String idempotencyKey, UUID merchantId);


    IdempotencyKey update(IdempotencyKey idempotencyKey);
}
