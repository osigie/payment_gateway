package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;

import java.util.UUID;

public interface PaymentService {

    public Result<Payment> createAuthorize(CreateAuthorizationRequestDto dto, UUID merchantId, String idempotencyKey, String requestPath);


}
