package com.osigie.payment_gateway.domain.entity;

import com.osigie.payment_gateway.domain.TransactionStatus;
import com.osigie.payment_gateway.domain.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    protected Transaction() {
    }


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount_minor")
    private long amount_minor;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "bank_reference")
    private String bankReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;


    public Transaction(Payment payment, long amount_minor, TransactionType type, TransactionStatus status, String bankReference) {
        this.payment = payment;
        this.amount_minor = amount_minor;
        this.type = type;
        this.status = status;
        this.bankReference = bankReference;
    }

    public UUID getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public long getAmount_minor() {
        return amount_minor;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getBankReference() {
        return bankReference;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

