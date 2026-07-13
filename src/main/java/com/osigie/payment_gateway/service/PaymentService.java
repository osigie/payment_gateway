package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;

import java.util.UUID;

public interface PaymentService {

    Result<PaymentResponse> createAuthorize(CreateAuthorizationRequestDto dto, UUID merchantId, String idempotencyKey, String requestPath);

    Result<PaymentResponse> createCapture(UUID paymentId, UUID merchantId, String idempotencyKey, String requestPath);
}
