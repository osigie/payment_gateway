package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.PaymentStatus;
import com.osigie.payment_gateway.domain.TransactionType;
import com.osigie.payment_gateway.domain.bank.recovery_points.AuthorizeRecoveryPoints;
import com.osigie.payment_gateway.domain.bank.recovery_points.CaptureRecoveryPoints;
import com.osigie.payment_gateway.domain.bank.recovery_points.RefundRecoveryPoints;
import com.osigie.payment_gateway.domain.bank.recovery_points.VoidRecoveryPoints;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.exception.BankBusinessException;
import com.osigie.payment_gateway.mapper.PaymentMapper;
import com.osigie.payment_gateway.repository.PaymentRepository;
import com.osigie.payment_gateway.repository.TransactionRepository;
import com.osigie.payment_gateway.service.impl.AtomicPhaseExecutorImpl;
import com.osigie.payment_gateway.service.impl.PaymentServiceImpl;
import com.osigie.payment_gateway.util.TestDataFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTests {

    @Mock
    private IdempotencyKeyService idempotencyKeyService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BankClient bankClient;
    @Mock
    private TransactionRepository transactionRepository;

    private PaymentMapper paymentMapper;
    private ObjectMapper objectMapper;
    private PaymentServiceImpl paymentService;

    private Merchant merchant;
    private Payment payment;
    private IdempotencyKey idempotencyKey;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        paymentMapper = new PaymentMapper();
        AtomicPhaseExecutor executor = new AtomicPhaseExecutorImpl(objectMapper, idempotencyKeyService);
        paymentService = new PaymentServiceImpl(
                executor, idempotencyKeyService, paymentRepository, objectMapper,
                bankClient, transactionRepository, paymentMapper
        );

        merchant = TestDataFactory.createTestMerchant();
        payment = TestDataFactory.createPayment(merchant, PaymentStatus.PENDING);
        idempotencyKey = TestDataFactory.createIdempotencyKey(merchant, null, AuthorizeRecoveryPoints.STARTED);
    }

    @Test
    void createAuthorize_withValidRequest_shouldProgressThroughAllPhases() {
        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any()))
                .thenReturn(idempotencyKey);
        when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
                .thenReturn(Optional.of(idempotencyKey));
        doNothing().when(idempotencyKeyService).update(any());

        when(paymentRepository.save(any())).thenAnswer((Answer<Payment>) invocation -> {
            Payment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        when(bankClient.authorize(any(), any())).thenReturn(TestDataFactory.createAuthorizeBankResponse());
        when(transactionRepository.save(any())).thenReturn(null);

        var result = paymentService.createAuthorize(
                TestDataFactory.createTestAuthorizationRequest(),
                merchant.getId(),
                "ikey-1",
                Operation.PAYMENT_AUTHORIZE
        );

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().status()).isEqualTo(PaymentStatus.AUTHORIZED);

        verify(bankClient).authorize(any(), any());
        verify(paymentRepository).save(any());
        verify(transactionRepository).save(any());
    }

    @Test
    void createAuthorize_withCachedResponse_shouldReturnCachedResult() {
        var cachedKey = TestDataFactory.createIdempotencyKey(merchant, payment, AuthorizeRecoveryPoints.FINISHED);
        cachedKey.setResponseBody(objectMapper.writeValueAsString(Result.success(paymentMapper.toDto(payment))));
        cachedKey.setResponseStatus(200);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any()))
                .thenReturn(cachedKey);

        var result = paymentService.createAuthorize(
                TestDataFactory.createTestAuthorizationRequest(),
                merchant.getId(),
                "ikey-cached",
                Operation.PAYMENT_AUTHORIZE
        );

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();

        verify(bankClient, never()).authorize(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createAuthorize_withBankError_shouldReturnFailure() {
        idempotencyKey.setRecoveryPoint(AuthorizeRecoveryPoints.BANK_AUTHORIZED);
        idempotencyKey.setPayment(payment);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any()))
                .thenReturn(idempotencyKey);
        when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
                .thenReturn(Optional.of(idempotencyKey));
        doNothing().when(idempotencyKeyService).update(any());

        when(bankClient.mapBankErrrorToErrorCode(any())).thenReturn(ErrorCode.INSUFFICIENT_FUNDS);
        when(bankClient.authorize(any(), any()))
                .thenThrow(new BankBusinessException("Insufficient funds", "insufficient_funds",
                        HttpStatus.PAYMENT_REQUIRED));

        var result = paymentService.createAuthorize(
                TestDataFactory.createTestAuthorizationRequest(),
                merchant.getId(),
                "ikey-error",
                Operation.PAYMENT_AUTHORIZE
        );

        assertThat(result.success()).isFalse();
        assertThat(result.error().code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(result.error().message()).contains("Insufficient funds");
    }

    @Test
    void createAuthorize_withUnknownRecoveryPoint_shouldThrowIllegalStateException() {
        idempotencyKey.setRecoveryPoint("UNKNOWN_POINT");

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any()))
                .thenReturn(idempotencyKey);

        Assertions.assertThatThrownBy(() ->
                        paymentService.createAuthorize(
                                TestDataFactory.createTestAuthorizationRequest(),
                                merchant.getId(),
                                "ikey-unknown",
                                Operation.PAYMENT_AUTHORIZE
                        )
                ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown recovery point");
    }

    @Test
    void createCapture_withValidRequest_shouldProgressThroughAllPhases() {
        payment.setStatus(PaymentStatus.AUTHORIZED);

        var captureKey = TestDataFactory.createIdempotencyKey(merchant, payment, CaptureRecoveryPoints.STARTED);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any(), any()))
                .thenReturn(captureKey);
        when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
                .thenReturn(Optional.of(captureKey));
        doNothing().when(idempotencyKeyService).update(any());

        var authTransaction = TestDataFactory.createTransaction(payment, TransactionType.AUTHORIZED, "auth-ref-123");
        when(transactionRepository.findByPaymentIdAndType(payment.getId(), TransactionType.AUTHORIZED))
                .thenReturn(Optional.of(authTransaction));

        when(bankClient.capture(any(), any())).thenReturn(TestDataFactory.createCaptureBankResponse());
        when(transactionRepository.save(any())).thenReturn(null);

        var result = paymentService.createCapture(
                payment.getId(), merchant.getId(), "ikey-cap-1", Operation.PAYMENT_CAPTURE
        );

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().status()).isEqualTo(PaymentStatus.CAPTURED);

        verify(bankClient).capture(any(), any());
    }

    @Test
    void createCapture_withNonAuthorizedPayment_shouldReturnFailure() {
        payment.setStatus(PaymentStatus.PENDING);

        var captureKey = TestDataFactory.createIdempotencyKey(merchant, payment, CaptureRecoveryPoints.STARTED);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any(), any()))
                .thenReturn(captureKey);

        var result = paymentService.createCapture(
                payment.getId(), merchant.getId(), "ikey-cap-2", Operation.PAYMENT_CAPTURE
        );

        assertThat(result.success()).isFalse();
        assertThat(result.error().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(result.error().message()).contains("Only authorized payment can be captured");
    }

    @Test
    void createVoid_withValidRequest_shouldProgressThroughAllPhases() {
        payment.setStatus(PaymentStatus.AUTHORIZED);

        var voidKey = TestDataFactory.createIdempotencyKey(merchant, payment, VoidRecoveryPoints.STARTED);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any(), any()))
                .thenReturn(voidKey);
        when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
                .thenReturn(Optional.of(voidKey));
        doNothing().when(idempotencyKeyService).update(any());

        var authTransaction = TestDataFactory.createTransaction(payment, TransactionType.AUTHORIZED, "auth-ref-123");
        when(transactionRepository.findByPaymentIdAndType(payment.getId(), TransactionType.AUTHORIZED))
                .thenReturn(Optional.of(authTransaction));

        when(bankClient._void(any(), any())).thenReturn(TestDataFactory.createVoidBankResponse());
        when(transactionRepository.save(any())).thenReturn(null);

        var result = paymentService.createVoid(
                payment.getId(), merchant.getId(), "ikey-void-1", Operation.PAYMENT_VOID
        );

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().status()).isEqualTo(PaymentStatus.VOIDED);

        verify(bankClient)._void(any(), any());
    }

    @Test
    void createVoid_withNonAuthorizedPayment_shouldReturnFailure() {
        payment.setStatus(PaymentStatus.CAPTURED);

        var voidKey = TestDataFactory.createIdempotencyKey(merchant, payment, VoidRecoveryPoints.STARTED);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any(), any()))
                .thenReturn(voidKey);

        var result = paymentService.createVoid(
                payment.getId(), merchant.getId(), "ikey-void-2", Operation.PAYMENT_VOID
        );

        assertThat(result.success()).isFalse();
        assertThat(result.error().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(result.error().message()).contains("Only authorized payment can be voided");
    }

    @Test
    void createRefund_withValidRequest_shouldProgressThroughAllPhases() {
        payment.setStatus(PaymentStatus.CAPTURED);

        var refundKey = TestDataFactory.createIdempotencyKey(merchant, payment, RefundRecoveryPoints.STARTED);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any(), any()))
                .thenReturn(refundKey);
        when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
                .thenReturn(Optional.of(refundKey));
        doNothing().when(idempotencyKeyService).update(any());

        var captureTransaction = TestDataFactory.createTransaction(payment, TransactionType.CAPTURE, "capture-ref-456");
        when(transactionRepository.findByPaymentIdAndType(payment.getId(), TransactionType.CAPTURE))
                .thenReturn(Optional.of(captureTransaction));

        when(bankClient.refund(any(), any())).thenReturn(TestDataFactory.createRefundBankResponse());
        when(transactionRepository.save(any())).thenReturn(null);

        var result = paymentService.createRefund(
                payment.getId(), merchant.getId(), "ikey-ref-1", Operation.PAYMENT_REFUND
        );

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().status()).isEqualTo(PaymentStatus.REFUNDED);

        verify(bankClient).refund(any(), any());
    }

    @Test
    void createRefund_withNonCapturedPayment_shouldReturnFailure() {
        payment.setStatus(PaymentStatus.AUTHORIZED);

        var refundKey = TestDataFactory.createIdempotencyKey(merchant, payment, RefundRecoveryPoints.STARTED);

        when(idempotencyKeyService.getOrCreateIdempotencyKey(any(), any(), any(), any(), any()))
                .thenReturn(refundKey);

        var result = paymentService.createRefund(
                payment.getId(), merchant.getId(), "ikey-ref-2", Operation.PAYMENT_REFUND
        );

        assertThat(result.success()).isFalse();
        assertThat(result.error().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(result.error().message()).contains("Only captured payment can be refunded");
    }

    @Test
    void getPayment_withExistingPayment_shouldReturnPayment() {
        payment.setStatus(PaymentStatus.AUTHORIZED);

        when(paymentRepository.findPaymentByIdAndMerchantId(payment.getId(), merchant.getId()))
                .thenReturn(Optional.of(payment));

        var result = paymentService.getPayment(payment.getId(), merchant.getId());

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(payment.getId());
    }

    @Test
    void getPayment_withNonExistingPayment_shouldReturnFailure() {
        when(paymentRepository.findPaymentByIdAndMerchantId(any(), any()))
                .thenReturn(Optional.empty());

        var result = paymentService.getPayment(UUID.randomUUID(), merchant.getId());

        assertThat(result.success()).isFalse();
        assertThat(result.error().code()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void getPayments_withFilters_shouldReturnFilteredResults() {
        payment.setStatus(PaymentStatus.AUTHORIZED);
        var payment2 = TestDataFactory.createPayment(merchant, PaymentStatus.CAPTURED);
        var page = new PageImpl<>(List.of(payment, payment2));

        when(paymentRepository.findAll(ArgumentMatchers.<Specification<Payment>>any(), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 10);
        var result = paymentService.getPayments("cust-456", "order-123", pageable, merchant.getId());

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }
}
