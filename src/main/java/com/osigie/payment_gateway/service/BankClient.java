package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.bank.CreateBankAuthorizationRequest;
import com.osigie.payment_gateway.domain.bank.CreateBankAuthorizationResponse;
import org.springframework.http.HttpStatusCode;

public interface BankClient {
    CreateBankAuthorizationResponse createAuthorization(CreateBankAuthorizationRequest request, String idempotencyKey);

    ErrorCode mapBankErrrorToErrorCode(HttpStatusCode status);
}
