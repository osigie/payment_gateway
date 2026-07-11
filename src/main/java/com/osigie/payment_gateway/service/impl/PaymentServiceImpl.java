package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.PhaseResult;
import com.osigie.payment_gateway.domain.StartAuthorizationContext;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.domain.recovery_points.AuthorizeRecoveryPoints;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import com.osigie.payment_gateway.repository.PaymentRepository;
import com.osigie.payment_gateway.service.AtomicPhaseExecutor;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final AtomicPhaseExecutor executor;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(AtomicPhaseExecutor executor, IdempotencyKeyRepository idempotencyKeyRepository, PaymentRepository paymentRepository, ObjectMapper objectMapper) {
        this.executor = executor;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Payment> createAuthorize(CreateAuthorizationRequestDto dto, UUID merchantId, String idempotencyKey) {
        while (true) {
//            TODO: handle exception class
            Optional<IdempotencyKey> optionalKey = idempotencyKeyRepository
                    .findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey);


            if (optionalKey.isEmpty()) {
                return Result.failure(
                        ErrorCode.IDEMPOTENCY_KEY_NOT_FOUND,
                        "Idempotency key not found"
                );
            }

            IdempotencyKey key = optionalKey.get();


            if (Objects.equals(key.getRecoveryPoint(), AuthorizeRecoveryPoints.FINISHED)) {
                return deserialize(key.getResponseBody(), new TypeReference<>() {
                });
            }

            StartAuthorizationContext context =
                    new StartAuthorizationContext(
                            merchantId,
                            idempotencyKey,
                            dto
                    );

            switch (key.getRecoveryPoint()) {
                case AuthorizeRecoveryPoints.STARTED -> {
                    executor.execute(merchantId, idempotencyKey, this::startAuthorization);
                }

                case AuthorizeRecoveryPoints.AUTHORIZATION_CREATED -> {
                    executor.execute(merchantId, idempotencyKey, k -> context, this::createAuthorization);
                }

                case AuthorizeRecoveryPoints.BANK_AUTHORIZED -> {
                    executor.execute(merchantId, idempotencyKey, k -> context, this::createBankAuthorization);
                }

                case AuthorizeRecoveryPoints.FINISHED -> {
                    executor.execute(merchantId, idempotencyKey, k -> context, this::finishAuthorization);
                }
                default -> throw new RuntimeException(
                        "Unknown recovery point "
                                + key.getRecoveryPoint());
            }
        }
    }


    private PhaseResult startAuthorization(IdempotencyKey IdempotencyKey) {

        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.AUTHORIZATION_CREATED
        );
    }

    private PhaseResult createAuthorization(StartAuthorizationContext context) {

        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.BANK_AUTHORIZED
        );
    }

    private PhaseResult createBankAuthorization(StartAuthorizationContext context) {
        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.FINISHED
        );
    }


    private PhaseResult finishAuthorization(StartAuthorizationContext context) {
//        Payment payment = paymentRepository.findById();
        return new PhaseResult.Response<>(
                Result.success(null));
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
