package com.osigie.payment_gateway.repository;

import com.osigie.payment_gateway.domain.TransactionType;
import com.osigie.payment_gateway.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByPaymentIdAndType(UUID id, TransactionType transactionType);

}
