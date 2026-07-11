package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.PaymentStatus;
import com.osigie.payment_gateway.domain.PhaseResult;
import com.osigie.payment_gateway.domain.StartAuthorizationContext;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.domain.recovery_points.AuthorizeRecoveryPoints;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.repository.MerchantRepository;
import com.osigie.payment_gateway.repository.PaymentRepository;
import com.osigie.payment_gateway.service.AtomicPhaseExecutor;
import com.osigie.payment_gateway.service.BankClient;
import com.osigie.payment_gateway.service.IdempotencyKeyService;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final AtomicPhaseExecutor executor;
    private final IdempotencyKeyService idempotencyKeyService;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final MerchantRepository merchantRepository;
    private final BankClient bankClient;

    public PaymentServiceImpl(AtomicPhaseExecutor executor,
                              IdempotencyKeyService idempotencyKeyService,
                              PaymentRepository paymentRepository, ObjectMapper objectMapper,
                              MerchantRepository merchantRepository, BankClient bankClient) {
        this.executor = executor;
        this.idempotencyKeyService = idempotencyKeyService;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.merchantRepository = merchantRepository;
        this.bankClient = bankClient;
    }

    @Override
    public Result<Payment> createAuthorize(CreateAuthorizationRequestDto dto, UUID merchantId, String idempotencyKey, String requestPath) {
        while (true) {
//            TODO: handle exception class
//            TODO: wire request details
            IdempotencyKey key = idempotencyKeyService
                    .getOrCreateIdempotencyKey(merchantId, idempotencyKey, objectMapper.writeValueAsString(dto), requestPath);


            if (Objects.equals(key.getRecoveryPoint(), AuthorizeRecoveryPoints.FINISHED)) {
                return deserialize(key.getResponseBody(), new TypeReference<>() {
                });
            }


            StartAuthorizationContext context =
                    new StartAuthorizationContext(
                            merchantId,
                            key,
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


    private PhaseResult startAuthorization(IdempotencyKey idempotencyKey) {

        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.AUTHORIZATION_CREATED
        );
    }

    private PhaseResult createAuthorization(StartAuthorizationContext context) {
//        TODO: exception
        Merchant merchant = merchantRepository.findById(context.merchantId())
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        Payment payment = new Payment(merchant,
                context.dto().merchantOrderId(),
                context.dto().merchantCustomerId(),
                context.dto().amountMinor(), "USD", PaymentStatus.PENDING);

        paymentRepository.save(payment);

        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());
        context.idempotencyKey().setPayment(payment);

        idempotencyKeyService.update(context.idempotencyKey());

        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.BANK_AUTHORIZED
        );
    }

    private PhaseResult createBankAuthorization(StartAuthorizationContext context) {
        try {

            context.idempotencyKey().setLastRunAt(OffsetDateTime.now());
            idempotencyKeyService.update(context.idempotencyKey());
            throw new RuntimeException();

//            return new PhaseResult.RecoveryPoint(
//                    AuthorizeRecoveryPoints.FINISHED
//            );
        } catch (Exception e) {
            return new PhaseResult.Response<>(Result.failure(ErrorCode.BANK_UNAVAILABLE, "Bank has not being implemented"));
        }

    }


    private PhaseResult finishAuthorization(StartAuthorizationContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());
        Payment payment = context.idempotencyKey().getPayment();
        payment.setStatus(PaymentStatus.AUTHORIZED);

        idempotencyKeyService.update(context.idempotencyKey());
        paymentRepository.save(payment);

        return new PhaseResult.Response<>(
                Result.success(payment));
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
