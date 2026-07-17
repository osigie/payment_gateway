package com.osigie.payment_gateway.repository;

import com.osigie.payment_gateway.domain.TransactionType;
import com.osigie.payment_gateway.domain.entity.Transaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
  Optional<Transaction> findByPaymentIdAndType(UUID id, TransactionType transactionType);
}
