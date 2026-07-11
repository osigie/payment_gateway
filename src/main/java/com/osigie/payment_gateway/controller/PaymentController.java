package com.osigie.payment_gateway.controller;

import com.osigie.payment_gateway.domain.entity.Payment;
import com.osigie.payment_gateway.dto.BaseResponse;
import com.osigie.payment_gateway.dto.ResponseMapper;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.dto.payment.PaymentResponse;
import com.osigie.payment_gateway.mapper.PaymentMapper;
import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/payments")
public class PaymentController {

    //TODO: add x-api-key middleware and x-idempotency-key to all POST and add validation


    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<PaymentResponse>> createAuthorize(@RequestBody CreateAuthorizationRequestDto dto) {
        Result<Payment> payment = paymentService.createAuthorize(dto);
        Result<PaymentResponse> paymentDto = payment.map(paymentMapper::toDto);
        return ResponseMapper.toResponse(paymentDto);
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
