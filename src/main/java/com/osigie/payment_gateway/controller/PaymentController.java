package com.osigie.payment_gateway.controller;

import com.osigie.payment_gateway.domain.MerchantPrincipal;
import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.dto.BaseResponse;
import com.osigie.payment_gateway.dto.ResponseMapper;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;
import com.osigie.payment_gateway.mapper.PaymentMapper;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<PaymentResponse>> createAuthorize(@RequestBody CreateAuthorizationRequestDto dto, @AuthenticationPrincipal MerchantPrincipal merchantPrincipal, @RequestHeader("x-idempotency-key") String idempotencyKey) {
        Result<Payment> payment = paymentService.createAuthorize(dto, merchantPrincipal.merchantId(), idempotencyKey);
        Result<PaymentResponse> paymentDto = payment.map(paymentMapper::toDto);
        return ResponseMapper.toResponse(paymentDto);
    }


    @GetMapping("/test")
    public ResponseEntity<String> test(
    ) {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("{paymentId}")
    public ResponseEntity<?> getPayment(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(null);
    }

    @GetMapping
    public ResponseEntity<?> getPayments(@RequestParam(required = false) String merchantOrderId,
                                         @RequestParam(required = false) String merchantCustomerId) {
        return ResponseEntity.ok(null);
    }


    @PostMapping("{paymentId}/capture")
    public ResponseEntity<?> createCapture(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(null);
    }


    @PostMapping("{paymentId}/refund")
    public ResponseEntity<?> createRefund(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(null);
    }

    @PostMapping("{paymentId}/void")
    public ResponseEntity<?> createVoid(@PathVariable("paymentId") UUID paymentId) {
        return ResponseEntity.ok(null);
    }


}
