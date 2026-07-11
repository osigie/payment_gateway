package com.osigie.payment_gateway.service.impl;

import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Override
    public Result<Payment> createAuthorize(CreateAuthorizationRequestDto dto) {
        return null;
    }
}
