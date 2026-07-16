package com.osigie.payment_gateway.controller;

import com.osigie.payment_gateway.domain.MerchantPrincipal;
import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.dto.BaseResponse;
import com.osigie.payment_gateway.dto.PageResponse;
import com.osigie.payment_gateway.dto.ResponseMapper;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;
import com.osigie.payment_gateway.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<PaymentResponse>> createAuthorize(
            @Valid @RequestBody CreateAuthorizationRequestDto dto,
            @AuthenticationPrincipal MerchantPrincipal merchantPrincipal,
            @RequestHeader("x-idempotency-key") String idempotencyKey) {

        Result<PaymentResponse> paymentResponse = paymentService.createAuthorize(
                dto, merchantPrincipal.merchantId(), idempotencyKey, Operation.PAYMENT_AUTHORIZE);

        return ResponseMapper.toResponse(paymentResponse);
    }


    @GetMapping("{paymentId}")
    public ResponseEntity<BaseResponse<PaymentResponse>> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal MerchantPrincipal merchantPrincipal) {
        Result<PaymentResponse> paymentResponse = paymentService.getPayment(
                paymentId, merchantPrincipal.merchantId());
        return ResponseMapper.toResponse(paymentResponse);
    }


    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(required = false) String merchantOrderId,
            @RequestParam(required = false) String merchantCustomerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal MerchantPrincipal merchantPrincipal) {

        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> payments = paymentService.getPayments(
                merchantCustomerId, merchantOrderId, pageable, merchantPrincipal.merchantId());

        PageResponse<PaymentResponse> response = new PageResponse<>(
                payments.getTotalElements(),
                payments.getNumber(),
                payments.getSize(),
                payments.getContent());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PostMapping("{paymentId}/capture")
    public ResponseEntity<BaseResponse<PaymentResponse>> createCapture(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal MerchantPrincipal merchantPrincipal,
            @RequestHeader("x-idempotency-key") String idempotencyKey) {

        Result<PaymentResponse> paymentResponse = paymentService.createCapture(
                paymentId, merchantPrincipal.merchantId(), idempotencyKey, Operation.PAYMENT_CAPTURE);

        return ResponseMapper.toResponse(paymentResponse);
    }

    @PostMapping("{paymentId}/void")
    public ResponseEntity<BaseResponse<PaymentResponse>> createVoid(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal MerchantPrincipal merchantPrincipal,
            @RequestHeader("x-idempotency-key") String idempotencyKey) {

        Result<PaymentResponse> paymentResponse = paymentService.createVoid(
                paymentId, merchantPrincipal.merchantId(), idempotencyKey, Operation.PAYMENT_VOID);

        return ResponseMapper.toResponse(paymentResponse);
    }


    @PostMapping("{paymentId}/refund")
    public ResponseEntity<BaseResponse<PaymentResponse>> createRefund(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal MerchantPrincipal merchantPrincipal,
            @RequestHeader("x-idempotency-key") String idempotencyKey) {

        Result<PaymentResponse> paymentResponse = paymentService.createRefund(
                paymentId, merchantPrincipal.merchantId(), idempotencyKey, Operation.PAYMENT_REFUND);

        return ResponseMapper.toResponse(paymentResponse);

    }


}
