package com.osigie.payment_gateway.util;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.PaymentStatus;
import com.osigie.payment_gateway.domain.TransactionStatus;
import com.osigie.payment_gateway.domain.TransactionType;
import com.osigie.payment_gateway.domain.bank.AuthorizeBankResponse;
import com.osigie.payment_gateway.domain.bank.CaptureBankResponse;
import com.osigie.payment_gateway.domain.bank.RefundBankResponse;
import com.osigie.payment_gateway.domain.bank.VoidBankResponse;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.domain.entity.Transaction;
import com.osigie.payment_gateway.dto.payment.CardDetailsDto;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class TestDataFactory {

  public static final UUID TEST_MERCHANT_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");
  public static final UUID TEST_PAYMENT_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  public static final String TEST_API_KEY = "test-api-key-12345";
  public static final String TEST_IDEMPOTENCY_KEY = "test-idempotency-key";

  public static Merchant createTestMerchant() {
    return createTestMerchant(TEST_MERCHANT_ID);
  }

  public static Merchant createTestMerchant(UUID merchantId) {
    var merchant = new Merchant("Test Merchant", TEST_API_KEY);
    ReflectionTestUtils.setField(merchant, "id", merchantId);
    return merchant;
  }

  public static Payment createPayment(Merchant merchant, PaymentStatus status) {
    return createPayment(merchant, status, TEST_PAYMENT_ID);
  }

  public static Payment createPayment(Merchant merchant, PaymentStatus status, UUID paymentId) {
    var payment = new Payment(merchant, "order-123", "cust-456", 5000L, "USD", status);
    ReflectionTestUtils.setField(payment, "id", paymentId);
    return payment;
  }

  public static IdempotencyKey createIdempotencyKey(
      Merchant merchant, Payment payment, String recoveryPoint) {
    var key =
        new IdempotencyKey(
            merchant,
            TEST_IDEMPOTENCY_KEY,
            "{}",
            recoveryPoint,
            OffsetDateTime.now(),
            Operation.PAYMENT_AUTHORIZE);
    if (payment != null) {
      key.setPayment(payment);
    }
    return key;
  }

  public static Transaction createTransaction(
      Payment payment, TransactionType type, String bankReference) {
    return new Transaction(
        payment, payment.getAmountMinor(), type, TransactionStatus.SUCCESS, bankReference);
  }

  public static CardDetailsDto createTestCardDetails() {
    return new CardDetailsDto("4111111111111111", "123", 12, 2028);
  }

  public static CreateAuthorizationRequestDto createTestAuthorizationRequest() {
    return new CreateAuthorizationRequestDto(
        "order-123", "cust-456", 5000L, createTestCardDetails());
  }

  public static AuthorizeBankResponse createAuthorizeBankResponse() {
    return new AuthorizeBankResponse(
        5000L, "auth-ref-123", OffsetDateTime.now(), OffsetDateTime.now().plusDays(1), "SUCCESS");
  }

  public static CaptureBankResponse createCaptureBankResponse() {
    return new CaptureBankResponse(
        5000L, "auth-ref-123", "capture-ref-456", OffsetDateTime.now(), "SUCCESS");
  }

  public static VoidBankResponse createVoidBankResponse() {
    return new VoidBankResponse("auth-ref-123", "SUCCESS", "void-ref-789", OffsetDateTime.now());
  }

  public static RefundBankResponse createRefundBankResponse() {
    return new RefundBankResponse(
        5000L, "capture-ref-456", "USD", "refund-ref-012", "SUCCESS", OffsetDateTime.now());
  }
}
