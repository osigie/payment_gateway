package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import com.osigie.payment_gateway.repository.MerchantRepository;
import com.osigie.payment_gateway.service.IdempotencyKeyService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyKeyServiceImpl implements IdempotencyKeyService {

    public static final String STARTED = "STARTED";

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final MerchantRepository merchantRepository;

    public IdempotencyKeyServiceImpl(IdempotencyKeyRepository idempotencyKeyRepository, MerchantRepository merchantRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.merchantRepository = merchantRepository;
    }

    @Override
    public IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, String requestParams, String requestPath) {

        return idempotencyKeyRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey)
                .orElseGet(() -> {

                    Merchant merchant = merchantRepository.findById(merchantId)
                            .orElseThrow(() -> new RuntimeException("Merchant not found"));

                    IdempotencyKey key = new IdempotencyKey(merchant, idempotencyKey, requestParams, requestPath, STARTED, OffsetDateTime.now());
                    idempotencyKeyRepository.save(key);
                    return key;
                });
    }


    @Override
    public Optional<IdempotencyKey> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey) {
        return idempotencyKeyRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey);
    }

    @Override
    public Optional<IdempotencyKey> findIdempotencyForUpdate(String idempotencyKey, UUID merchantId) {
        return idempotencyKeyRepository.findIdempotencyForUpdate(idempotencyKey, merchantId);
    }

    @Override
    public IdempotencyKey update(IdempotencyKey idempotencyKey) {
        return idempotencyKeyRepository.save(idempotencyKey);
    }
}
