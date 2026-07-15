package com.osigie.payment_gateway.repository.spec;

import com.osigie.payment_gateway.domain.entity.Payment;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class PaymentSpecification {

    public static Specification<Payment> belongsToMerchant(UUID merchantId) {
        return (root, query, cb) ->
                cb.equal(root.get("merchant").get("id"), merchantId);
    }

    public static Specification<Payment> belongsToMerchantCustomer(String merchantCustomerId) {
        return (root, query, cb) -> merchantCustomerId == null || merchantCustomerId.isBlank() ? null : cb.equal(root.get("merchantCustomerId"), merchantCustomerId);
    }

    public static Specification<Payment> belongsToMerchantOrder(String merchantOrderId) {
        return (root, query, cb) -> merchantOrderId == null || merchantOrderId.isBlank() ? null : cb.equal(root.get("merchantOrderId"), merchantOrderId);
    }

}
