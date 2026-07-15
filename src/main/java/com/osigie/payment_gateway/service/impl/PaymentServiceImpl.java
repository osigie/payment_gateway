package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.*;
import com.osigie.payment_gateway.domain.bank.*;
import com.osigie.payment_gateway.domain.bank.context.AuthorizationContext;
import com.osigie.payment_gateway.domain.bank.context.CaptureContext;
import com.osigie.payment_gateway.domain.bank.context.RefundContext;
import com.osigie.payment_gateway.domain.bank.context.VoidContext;
import com.osigie.payment_gateway.domain.bank.recovery_points.RefundRecoveryPoints;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.domain.entity.Transaction;
import com.osigie.payment_gateway.domain.bank.recovery_points.AuthorizeRecoveryPoints;
import com.osigie.payment_gateway.domain.bank.recovery_points.CaptureRecoveryPoints;
import com.osigie.payment_gateway.domain.bank.recovery_points.VoidRecoveryPoints;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;
import com.osigie.payment_gateway.exception.BankBusinessException;
import com.osigie.payment_gateway.exception.ResourceNotFoundException;
import com.osigie.payment_gateway.mapper.PaymentMapper;
import com.osigie.payment_gateway.repository.PaymentRepository;
import com.osigie.payment_gateway.repository.TransactionRepository;
import com.osigie.payment_gateway.repository.spec.PaymentSpecification;
import com.osigie.payment_gateway.service.AtomicPhaseExecutor;
import com.osigie.payment_gateway.service.BankClient;
import com.osigie.payment_gateway.service.IdempotencyKeyService;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new AuthorizationContext(
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
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new AuthorizationContext(
                            merchantId,
                            k,
                            dto
                    ), this::createBankAuthorization);
                }

                case AuthorizeRecoveryPoints.BANK_AUTHORIZATION_COMPLETED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new AuthorizationContext(
                            merchantId,
                            k,
                            dto
                    ), this::completeBankAuthorization);
                }

                case AuthorizeRecoveryPoints.FINISHED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new AuthorizationContext(
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

            AuthorizeBankRequest request = new AuthorizeBankRequest(context.idempotencyKey().getPayment().getAmountMinor(), context.dto().cardDetails().cardNumber(), context.dto().cardDetails().cvv(), context.dto().cardDetails().expiryMonth(), context.dto().cardDetails().expiryYear());

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


            if (key.hasCachedResponse(CaptureRecoveryPoints.FINISHED)) {
                return deserialize(key.getResponseBody(), new TypeReference<>() {
                });
            }

            Payment payment = key.getPayment();

            if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
                return Result.failure(ErrorCode.BAD_REQUEST, "Only authorized payment can be captured");
            }


            Transaction authorizationTransaction = transactionRepository.findByPaymentIdAndType(key.getPayment().getId(), TransactionType.AUTHORIZED).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));


            switch (key.getRecoveryPoint()) {
                case CaptureRecoveryPoints.STARTED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new CaptureContext(
                            authorizationTransaction.getBankReference(),
                            authorizationTransaction.getAmountMinor(),
                            k
                    ), this::startCapture);
                }

                case CaptureRecoveryPoints.BANK_CAPTURE -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new CaptureContext(
                            authorizationTransaction.getBankReference(),
                            authorizationTransaction.getAmountMinor(),
                            k
                    ), this::createBankCapture);
                }

                case CaptureRecoveryPoints.BANK_CAPTURE_COMPLETED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new CaptureContext(
                            authorizationTransaction.getBankReference(),
                            authorizationTransaction.getAmountMinor(),
                            k
                    ), this::completeBankCapture);
                }

                case CaptureRecoveryPoints.FINISHED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new CaptureContext(
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

    @Override
    public Result<PaymentResponse> createVoid(UUID paymentId, UUID merchantId, String idempotencyKey, String requestPath) {

        while (true) {
            IdempotencyKey key = idempotencyKeyService
                    .getOrCreateIdempotencyKey(merchantId, idempotencyKey, paymentId, objectMapper.writeValueAsString(paymentId), requestPath);


            if (key.hasCachedResponse(VoidRecoveryPoints.FINISHED)) {
                return deserialize(key.getResponseBody(), new TypeReference<>() {
                });
            }

            Payment payment = key.getPayment();

            if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
                return Result.failure(ErrorCode.BAD_REQUEST, "Only authorized payment can be voided");
            }

            Transaction authorizationTransaction = transactionRepository.findByPaymentIdAndType(key.getPayment().getId(), TransactionType.AUTHORIZED).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));


            switch (key.getRecoveryPoint()) {
                case VoidRecoveryPoints.STARTED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new VoidContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::startVoid);
                }

                case VoidRecoveryPoints.BANK_VOID -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new VoidContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::createBankVoid);
                }

                case VoidRecoveryPoints.BANK_VOID_COMPLETED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new VoidContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::completeBankVoid);
                }

                case VoidRecoveryPoints.FINISHED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new VoidContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::finishVoid);
                }

                default -> throw new IllegalStateException(
                        "Unknown recovery point "
                                + key.getRecoveryPoint());

            }
        }
    }


    private PhaseResult startVoid(VoidContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        return new PhaseResult.RecoveryPoint(
                VoidRecoveryPoints.BANK_VOID
        );
    }

    private PhaseResult createBankVoid(VoidContext context) {

        try {
            context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

            VoidBankRequest request = new VoidBankRequest(context.authorizationRefId());

            VoidBankResponse response = bankClient._void(request, "void:" + context.idempotencyKey().getIdempotencyKey());

            Transaction transaction = new Transaction(context.idempotencyKey().getPayment(), context.idempotencyKey().getPayment().getAmountMinor(), TransactionType.VOID, TransactionStatus.SUCCESS, response.voidId());

            transactionRepository.save(transaction);

            return new PhaseResult.RecoveryPoint(
                    VoidRecoveryPoints.BANK_VOID_COMPLETED
            );
        } catch (BankBusinessException ex) {
            return new PhaseResult.Response<>(Result.failure(bankClient.mapBankErrrorToErrorCode(ex.getStatus()), ex.getMessage()));
        }
    }

    private PhaseResult completeBankVoid(VoidContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        return new PhaseResult.RecoveryPoint(
                VoidRecoveryPoints.FINISHED
        );
    }

    private PhaseResult finishVoid(VoidContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        Payment payment = context.idempotencyKey().getPayment();
        payment.setStatus(PaymentStatus.VOIDED);

        return new PhaseResult.Response<>(
                Result.success(paymentMapper.toDto(payment)));
    }


    @Override
    public Result<PaymentResponse> createRefund(UUID paymentId, UUID merchantId, String idempotencyKey, String requestPath) {
        while (true) {
            IdempotencyKey key = idempotencyKeyService
                    .getOrCreateIdempotencyKey(merchantId, idempotencyKey, paymentId, objectMapper.writeValueAsString(paymentId), requestPath);


            if (key.hasCachedResponse(RefundRecoveryPoints.FINISHED)) {
                return deserialize(key.getResponseBody(), new TypeReference<>() {
                });
            }

            Payment payment = key.getPayment();

            if (payment.getStatus() != PaymentStatus.CAPTURED) {
                return Result.failure(ErrorCode.BAD_REQUEST, "Only captured payment can be refunded");
            }

            Transaction authorizationTransaction = transactionRepository.findByPaymentIdAndType(key.getPayment().getId(), TransactionType.CAPTURE).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));


            switch (key.getRecoveryPoint()) {
                case RefundRecoveryPoints.STARTED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new RefundContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::startRefund);
                }

                case RefundRecoveryPoints.BANK_REFUND -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new RefundContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::createBankRefund);
                }

                case RefundRecoveryPoints.BANK_REFUND_COMPLETED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new RefundContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::completeBankRefund);
                }

                case RefundRecoveryPoints.FINISHED -> {
                    executor.execute(merchantId, idempotencyKey, requestPath, k -> new RefundContext(
                            authorizationTransaction.getBankReference(),
                            k
                    ), this::finishRefund);
                }

                default -> throw new IllegalStateException(
                        "Unknown recovery point "
                                + key.getRecoveryPoint());

            }
        }
    }


    private PhaseResult startRefund(RefundContext context) {

        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        return new PhaseResult.RecoveryPoint(
                RefundRecoveryPoints.BANK_REFUND
        );
    }

    private PhaseResult createBankRefund(RefundContext context) {

        try {
            context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

            RefundBankRequest request = new RefundBankRequest(context.captureRefId(), context.idempotencyKey().getPayment().getAmountMinor());

            RefundBankResponse response = bankClient.refund(request, "refund:" + context.idempotencyKey().getIdempotencyKey());

            Transaction transaction = new Transaction(context.idempotencyKey().getPayment(), request.amount(), TransactionType.REFUND, TransactionStatus.SUCCESS, response.refundId());

            transactionRepository.save(transaction);

            return new PhaseResult.RecoveryPoint(
                    RefundRecoveryPoints.BANK_REFUND_COMPLETED
            );
        } catch (BankBusinessException ex) {
            return new PhaseResult.Response<>(Result.failure(bankClient.mapBankErrrorToErrorCode(ex.getStatus()), ex.getMessage()));
        }
    }

    private PhaseResult completeBankRefund(RefundContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        return new PhaseResult.RecoveryPoint(
                RefundRecoveryPoints.FINISHED
        );
    }

    private PhaseResult finishRefund(RefundContext context) {
        context.idempotencyKey().setLastRunAt(OffsetDateTime.now());

        Payment payment = context.idempotencyKey().getPayment();
        payment.setStatus(PaymentStatus.REFUNDED);

        return new PhaseResult.Response<>(
                Result.success(paymentMapper.toDto(payment)));
    }


    @Override
    public Result<PaymentResponse> getPayment(UUID paymentId, UUID merchantId) {
        Optional<Payment> payment = paymentRepository.findPaymentByIdAndMerchantId(paymentId, merchantId);
        return payment.map(value -> Result.success(paymentMapper.toDto(value))).orElseGet(() -> Result.failure(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found"));

    }

    @Override
    public Page<PaymentResponse> getPayments(String merchantCustomerId, String merchantOrderId, Pageable pageable, UUID merchantId) {

        Specification<Payment> spec =
                Specification.where(PaymentSpecification.belongsToMerchant(merchantId))
                        .and(PaymentSpecification.belongsToMerchantCustomer(merchantCustomerId))
                        .and(PaymentSpecification.belongsToMerchantOrder(merchantOrderId));

        return paymentRepository.findAll(spec, pageable).map(paymentMapper::toDto);

    }


    private <T> T deserialize(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
