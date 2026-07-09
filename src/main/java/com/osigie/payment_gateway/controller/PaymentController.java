package com.osigie.payment_gateway.controller;

import com.osigie.payment_gateway.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/payments")
public class PaymentController {

    //TODO: add x-api-key middleware and x-idempotency-key to all POST and add validation


    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody Object payment) {
        return ResponseEntity.ok(null);
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
