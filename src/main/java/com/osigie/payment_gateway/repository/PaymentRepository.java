package com.osigie.payment_gateway.repository;

import com.osigie.payment_gateway.domain.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository
    extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

  Optional<Payment> findPaymentByIdAndMerchantId(UUID paymentId, UUID merchantId);
}
