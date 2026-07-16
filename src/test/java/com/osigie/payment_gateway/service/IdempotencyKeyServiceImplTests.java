package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.exception.ResourceNotFoundException;
import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import com.osigie.payment_gateway.repository.MerchantRepository;
import com.osigie.payment_gateway.repository.PaymentRepository;
import com.osigie.payment_gateway.service.impl.IdempotencyKeyServiceImpl;
import com.osigie.payment_gateway.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyServiceImplTests {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private IdempotencyKeyServiceImpl idempotencyKeyService;

    @Test
    void getOrCreateIdempotencyKey_withNewKey_shouldCreateKey() {
        var merchant = TestDataFactory.createTestMerchant();
        var key = TestDataFactory.createIdempotencyKey(merchant, null, "STARTED");

        when(idempotencyKeyRepository.findByMerchantIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(merchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));
        when(idempotencyKeyRepository.save(any())).thenReturn(key);

        var result = idempotencyKeyService.getOrCreateIdempotencyKey(
                merchant.getId(), "new-key", "{}", Operation.PAYMENT_AUTHORIZE
        );

        assertThat(result).isNotNull();
        assertThat(result.getIdempotencyKey()).isEqualTo("new-key");
        assertThat(result.getRecoveryPoint()).isEqualTo("STARTED");
        verify(idempotencyKeyRepository).save(any());
    }

    @Test
    void getOrCreateIdempotencyKey_withExistingKey_shouldReturnExistingKey() {
        var merchant = TestDataFactory.createTestMerchant();
        var key = TestDataFactory.createIdempotencyKey(merchant, null, "STARTED");

        when(idempotencyKeyRepository.findByMerchantIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.of(key));

        var result = idempotencyKeyService.getOrCreateIdempotencyKey(
                merchant.getId(), "existing-key", "{}", Operation.PAYMENT_AUTHORIZE
        );

        assertThat(result).isEqualTo(key);
        verify(idempotencyKeyRepository, never()).save(any());
    }

    @Test
    void getOrCreateIdempotencyKey_withMerchantNotFound_shouldThrowResourceNotFoundException() {
        when(idempotencyKeyRepository.findByMerchantIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(merchantRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                idempotencyKeyService.getOrCreateIdempotencyKey(
                        TestDataFactory.TEST_MERCHANT_ID, "new-key", "{}", Operation.PAYMENT_AUTHORIZE
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Merchant not found");
    }

    @Test
    void getOrCreateIdempotencyKey_withPaymentAndNewKey_shouldCreateKey() {
        var merchant = TestDataFactory.createTestMerchant();
        var payment = TestDataFactory.createPayment(merchant, com.osigie.payment_gateway.domain.PaymentStatus.PENDING);

        when(idempotencyKeyRepository.findByMerchantIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));
        when(idempotencyKeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = idempotencyKeyService.getOrCreateIdempotencyKey(
                merchant.getId(), "new-key-payment", payment.getId(), "{}", Operation.PAYMENT_CAPTURE
        );

        assertThat(result).isNotNull();
        assertThat(result.getPayment()).isEqualTo(payment);
        assertThat(result.getOperation()).isEqualTo(Operation.PAYMENT_CAPTURE);
        verify(idempotencyKeyRepository).save(any());
    }

    @Test
    void getOrCreateIdempotencyKey_withPaymentNotFound_shouldThrowResourceNotFoundException() {
        when(idempotencyKeyRepository.findByMerchantIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                idempotencyKeyService.getOrCreateIdempotencyKey(
                        TestDataFactory.TEST_MERCHANT_ID, "new-key", TestDataFactory.TEST_PAYMENT_ID,
                        "{}", Operation.PAYMENT_CAPTURE
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void findIdempotencyForUpdate_shouldDelegateToRepository() {
        var merchant = TestDataFactory.createTestMerchant();
        var key = TestDataFactory.createIdempotencyKey(merchant, null, "STARTED");

        when(idempotencyKeyRepository.findIdempotencyForUpdate("ikey", merchant.getId(), Operation.PAYMENT_AUTHORIZE))
                .thenReturn(Optional.of(key));

        var result = idempotencyKeyService.findIdempotencyForUpdate("ikey", merchant.getId(), Operation.PAYMENT_AUTHORIZE);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(key);
    }

    @Test
    void update_shouldDelegateToRepository() {
        var merchant = TestDataFactory.createTestMerchant();
        var key = TestDataFactory.createIdempotencyKey(merchant, null, "STARTED");

        idempotencyKeyService.update(key);

        verify(idempotencyKeyRepository).save(key);
    }
}
