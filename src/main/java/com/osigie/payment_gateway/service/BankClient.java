package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.bank.*;
import org.springframework.http.HttpStatusCode;

public interface BankClient {
    AuthorizeBankResponse authorize(AuthorizeBankRequest request, String idempotencyKey);

    ErrorCode mapBankErrrorToErrorCode(HttpStatusCode status);

    public CaptureBankResponse capture(CaptureBankRequest request, String idempotencyKey);

    VoidBankResponse _void(VoidBankRequest request, String idempotencyKey);
}
