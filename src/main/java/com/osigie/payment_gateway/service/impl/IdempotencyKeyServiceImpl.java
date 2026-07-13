package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.IdempotencyKeyType;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.exception.ResourceNotFoundException;
import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import com.osigie.payment_gateway.repository.MerchantRepository;
import com.osigie.payment_gateway.repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository;

    public IdempotencyKeyServiceImpl(IdempotencyKeyRepository idempotencyKeyRepository, MerchantRepository merchantRepository, PaymentRepository paymentRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, String requestParams, String requestPath) {

        return idempotencyKeyRepository.findByMerchantIdAndIdempotencyKeyAndRequestPath(merchantId, idempotencyKey, requestPath)
                .orElseGet(() -> {

                    Merchant merchant = merchantRepository.findById(merchantId)
                            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

                    IdempotencyKey key = new IdempotencyKey(merchant, idempotencyKey, requestParams, requestPath, STARTED, OffsetDateTime.now());

                    idempotencyKeyRepository.save(key);
                    return key;
                });
    }

    @Override
    public IdempotencyKey getOrCreateIdempotencyKey(UUID merchantId, String idempotencyKey, UUID paymentId, String requestParams, String requestPath) {

        return idempotencyKeyRepository.findByMerchantIdAndIdempotencyKeyAndRequestPath(merchantId, idempotencyKey, requestPath)
                .orElseGet(() -> {

                    Payment payment = paymentRepository.findById(paymentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

                    IdempotencyKey key = new IdempotencyKey(
                            payment.getMerchant(),
                            idempotencyKey,
                            requestParams,
                            requestPath,
                            STARTED,
                            OffsetDateTime.now()
                    );
                    key.setPayment(payment);
                    return idempotencyKeyRepository.save(key);
                });

    }


    @Override
    public Optional<IdempotencyKey> findIdempotencyForUpdate(String idempotencyKey, UUID merchantId, String requestPath) {
        return idempotencyKeyRepository.findIdempotencyForUpdate(idempotencyKey, merchantId, requestPath);
    }

    @Override
    public void update(IdempotencyKey idempotencyKey) {
        idempotencyKeyRepository.save(idempotencyKey);
    }
}
