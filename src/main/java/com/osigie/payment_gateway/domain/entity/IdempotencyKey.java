package com.osigie.payment_gateway.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    protected IdempotencyKey() {
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_params", nullable = false, columnDefinition = "JSONB")
    private String requestParams;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "response_status")
    private int responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "JSONB")
    private String responseBody;

    @Column(name = "recovery_point", nullable = false)
    private String recoveryPoint;


    @Column(name = "last_run_at", nullable = false, updatable = false)
    private OffsetDateTime lastRunAt;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


    public IdempotencyKey(Merchant merchant, String idempotencyKey, String requestParams, String requestPath, String recoveryPoint, OffsetDateTime lastRunAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestParams = requestParams;
        this.requestPath = requestPath;
        this.recoveryPoint = recoveryPoint;
        this.lastRunAt = lastRunAt;
        this.merchant = merchant;
    }


    public UUID getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getRecoveryPoint() {
        return recoveryPoint;
    }

    public OffsetDateTime getLastRunAt() {
        return lastRunAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }


    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setLastRunAt(OffsetDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public void setRecoveryPoint(String recoveryPoint) {
        this.recoveryPoint = recoveryPoint;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public boolean hasCachedResponse(String finishedRecoveryPoint) {
        return Objects.equals(recoveryPoint, finishedRecoveryPoint)
                && responseBody != null;
    }
}
