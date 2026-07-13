package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.*;
import com.osigie.payment_gateway.domain.bank.AuthorizeBankRequest;
import com.osigie.payment_gateway.domain.bank.AuthorizeBankResponse;
import com.osigie.payment_gateway.domain.bank.CaptureBankRequest;
import com.osigie.payment_gateway.domain.bank.CaptureBankResponse;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.domain.entity.Transaction;
import com.osigie.payment_gateway.domain.recovery_points.AuthorizeRecoveryPoints;
import com.osigie.payment_gateway.domain.recovery_points.CaptureRecoveryPoints;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;
import com.osigie.payment_gateway.exception.BankBusinessException;
import com.osigie.payment_gateway.exception.ResourceNotFoundException;
import com.osigie.payment_gateway.mapper.PaymentMapper;
import com.osigie.payment_gateway.repository.PaymentRepository;
import com.osigie.payment_gateway.repository.TransactionRepository;
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
    private final BankClient bankClient;
    private final TransactionRepository transactionRepository;
    private final PaymentMapper paymentMapper;

    public PaymentServiceImpl(AtomicPhaseExecutor executor,
                              IdempotencyKeyService idempotencyKeyService,
                              PaymentRepository paymentRepository, ObjectMapper objectMapper,
                              BankClient bankClient, TransactionRepository transactionRepository, PaymentMapper paymentMapper) {
        this.executor = executor;
        this.idempotencyKeyService = idempotencyKeyService;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.bankClient = bankClient;
        this.transactionRepository = transactionRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public Result<PaymentResponse> createAuthorize(CreateAuthorizationRequestDto dto, UUID merchantId, String idempotencyKey, String requestPath) {
        while (true) {

            IdempotencyKey key = idempotencyKeyService
                    .getOrCreateIdempotencyKey(merchantId, idempotencyKey, objectMapper.writeValueAsString(dto), requestPath);


            if (Objects.equals(key.getRecoveryPoint(), AuthorizeRecoveryPoints.FINISHED) && key.getResponseBody() != null) {
                return deserialize(key.getResponseBody(), new TypeReference<>() {
                });
            }

            switch (key.getRecoveryPoint()) {
                case AuthorizeRecoveryPoints.STARTED -> {
                    executor.execute(merchantId, idempotencyKey,requestPath, k -> new AuthorizationContext(
                            merchantId,
                            k,
                            dto
                    ), this::startAuthorization);
                }

                case AuthorizeRecoveryPoints.AUTHORIZATION_CREATED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new AuthorizationContext(
                            merchantId,
                            k,
                            dto
                    ), this::createAuthorization);
                }

                case AuthorizeRecoveryPoints.BANK_AUTHORIZED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath,k -> new AuthorizationContext(
                            merchantId,
                            k,
                            dto
                    ), this::createBankAuthorization);
                }

                case AuthorizeRecoveryPoints.BANK_AUTHORIZATION_COMPLETED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath,k -> new AuthorizationContext(
                            merchantId,
                            k,
                            dto
                    ), this::completeBankAuthorization);
                }

                case AuthorizeRecoveryPoints.FINISHED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath,k -> new AuthorizationContext(
                            merchantId,
                            k,
                            dto
                    ), this::finishAuthorization);
                }
                default -> throw new IllegalStateException(
                        "Unknown recovery point "
                                + key.getRecoveryPoint());
            }
        }
    }


    private PhaseResult startAuthorization(AuthorizationContext context) {

        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());


        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.AUTHORIZATION_CREATED
        );

    }

    private PhaseResult createAuthorization(AuthorizationContext context) {


        Merchant merchant = context.idempotencyKey().getMerchant();

        Payment payment = new Payment(merchant,
                context.dto().merchantOrderId(),
                context.dto().merchantCustomerId(),
                context.dto().amountMinor(), "USD", PaymentStatus.PENDING);

        paymentRepository.save(payment);

        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());
        context.idempotencyKey().setPayment(payment);


        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.BANK_AUTHORIZED
        );
    }

    private PhaseResult createBankAuthorization(AuthorizationContext context) {
        try {

            context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

            AuthorizeBankRequest request = new AuthorizeBankRequest(context.dto().amountMinor(), context.dto().cardDetails().cardNumber(), context.dto().cardDetails().cvv(), context.dto().cardDetails().expiryMonth(), context.dto().cardDetails().expiryYear());

            AuthorizeBankResponse response = bankClient.authorize(request, "authorization:" + context.idempotencyKey().getIdempotencyKey());

            Transaction transaction = new Transaction(context.idempotencyKey().getPayment(), response.amount(), TransactionType.AUTHORIZED, TransactionStatus.SUCCESS, response.authorizationId());

            transactionRepository.save(transaction);

            return new PhaseResult.RecoveryPoint(
                    AuthorizeRecoveryPoints.BANK_AUTHORIZATION_COMPLETED
            );
        } catch (BankBusinessException ex) {
            return new PhaseResult.Response<>(Result.failure(bankClient.mapBankErrrorToErrorCode(ex.getStatus()), ex.getMessage()));
        }

    }


    private PhaseResult completeBankAuthorization(AuthorizationContext context) {

        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());


        return new PhaseResult.RecoveryPoint(
                AuthorizeRecoveryPoints.FINISHED
        );
    }

    private PhaseResult finishAuthorization(AuthorizationContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());
        Payment payment = context.idempotencyKey().getPayment();
        payment.setStatus(PaymentStatus.AUTHORIZED);

        return new PhaseResult.Response<>(
                Result.success(paymentMapper.toDto(payment)));
    }


    @Override
    public Result<PaymentResponse> createCapture(UUID paymentId, UUID merchantId, String idempotencyKey, String requestPath) {

        while (true) {

            IdempotencyKey key = idempotencyKeyService
                    .getOrCreateIdempotencyKey(merchantId, idempotencyKey, paymentId, objectMapper.writeValueAsString(paymentId), requestPath);


            if (Objects.equals(key.getRecoveryPoint(), CaptureRecoveryPoints.FINISHED) && key.getResponseBody() != null) {
                return deserialize(key.getResponseBody(), new TypeReference<>() {
                });
            }

            Transaction authorizationTransaction = transactionRepository.findByPaymentIdAndType(key.getPayment().getId(), TransactionType.AUTHORIZED).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));


            switch (key.getRecoveryPoint()) {
                case CaptureRecoveryPoints.STARTED -> {
                    executor.execute(merchantId, idempotencyKey,requestPath, k -> new CaptureContext(
                            authorizationTransaction.getBankReference(),
                            authorizationTransaction.getAmountMinor(),
                            k
                    ), this::startCapture);
                }

                case CaptureRecoveryPoints.BANK_CAPTURE -> {
                    executor.execute(merchantId, idempotencyKey,requestPath, k -> new CaptureContext(
                            authorizationTransaction.getBankReference(),
                            authorizationTransaction.getAmountMinor(),
                            k
                    ), this::createBankCapture);
                }

                case CaptureRecoveryPoints.BANK_CAPTURE_COMPLETED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath,k -> new CaptureContext(
                            authorizationTransaction.getBankReference(),
                            authorizationTransaction.getAmountMinor(),
                            k
                    ), this::completeBankCapture);
                }

                case CaptureRecoveryPoints.FINISHED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath,k -> new CaptureContext(
                            authorizationTransaction.getBankReference(),
                            authorizationTransaction.getAmountMinor(),
                            k
                    ), this::finishCapture);
                }

                default -> throw new IllegalStateException(
                        "Unknown recovery point "
                                + key.getRecoveryPoint());
            }
        }
    }

    private PhaseResult startCapture(CaptureContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        return new PhaseResult.RecoveryPoint(
                CaptureRecoveryPoints.BANK_CAPTURE
        );
    }

    private PhaseResult createBankCapture(CaptureContext context) {
        try {

            context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

            CaptureBankRequest request = new CaptureBankRequest(context.amount(), context.authorizationRefId());

            CaptureBankResponse response = bankClient.capture(request, "capture:" + context.idempotencyKey().getIdempotencyKey());

            Transaction transaction = new Transaction(context.idempotencyKey().getPayment(), response.amount(), TransactionType.CAPTURE, TransactionStatus.SUCCESS, response.captureId());

            transactionRepository.save(transaction);

            return new PhaseResult.RecoveryPoint(
                    CaptureRecoveryPoints.BANK_CAPTURE_COMPLETED
            );
        } catch (BankBusinessException ex) {
            return new PhaseResult.Response<>(Result.failure(bankClient.mapBankErrrorToErrorCode(ex.getStatus()), ex.getMessage()));
        }
    }

    private PhaseResult completeBankCapture(CaptureContext context) {

        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        return new PhaseResult.RecoveryPoint(
                CaptureRecoveryPoints.FINISHED
        );
    }

    private PhaseResult finishCapture(CaptureContext context) {

        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        Payment payment = context.idempotencyKey().getPayment();
        payment.setStatus(PaymentStatus.CAPTURED);

        return new PhaseResult.Response<>(
                Result.success(paymentMapper.toDto(payment)));
    }


    private <T> T deserialize(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
