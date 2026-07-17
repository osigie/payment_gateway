package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

  Result<PaymentResponse> createAuthorize(
      CreateAuthorizationRequestDto dto,
      UUID merchantId,
      String idempotencyKey,
      Operation operation);

  Result<PaymentResponse> createCapture(
      UUID paymentId, UUID merchantId, String idempotencyKey, Operation operation);

  Result<PaymentResponse> createRefund(
      UUID paymentId, UUID merchantId, String idempotencyKey, Operation operation);

  Result<PaymentResponse> createVoid(
      UUID paymentId, UUID merchantId, String idempotencyKey, Operation operation);

  Result<PaymentResponse> getPayment(UUID paymentId, UUID merchantId);

  Page<PaymentResponse> getPayments(
      String merchantCustomerId, String merchantOrderId, Pageable pageable, UUID merchantId);
}
