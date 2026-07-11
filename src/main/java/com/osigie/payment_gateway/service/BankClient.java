package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.bank.CreateBankAuthorizationRequest;
import com.osigie.payment_gateway.domain.bank.CreateBankAuthorizationResponse;

public interface BankClient {
    CreateBankAuthorizationResponse createAuthorization(CreateBankAuthorizationRequest request, String idempotencyKey);
}
