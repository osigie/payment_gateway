package com.osigie.payment_gateway.mapper;

import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

  public PaymentResponse toDto(Payment payment) {

    return new PaymentResponse(
        payment.getMerchantOrderId(),
        payment.getMerchantCustomerId(),
        payment.getAmountMinor(),
        payment.getStatus(),
        payment.getCreatedAt(),
        payment.getUpdatedAt(),
        payment.getId(),
        payment.getStatus());
  }
}
