package com.osigie.payment_gateway.domain;

public enum PaymentStatus {
  PENDING,
  AUTHORIZED,
  CAPTURED,
  FAILED,
  REFUNDED,
  VOIDED
}
