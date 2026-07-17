package com.osigie.payment_gateway.domain.entity;

import com.osigie.payment_gateway.domain.PaymentStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "payments")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  protected Payment() {}

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id", nullable = false)
  private Merchant merchant;

  @Column(name = "merchant_order_id")
  private String merchantOrderId;

  @Column(name = "merchant_customer_id")
  private String merchantCustomerId;

  @Column(name = "amount_minor")
  private long amountMinor;

  @Column(name = "currency")
  private String currency;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public Payment(
      Merchant merchant,
      String merchantOrderId,
      String merchantCustomerId,
      long amountMinor,
      String currency,
      PaymentStatus status) {
    this.merchant = merchant;
    this.merchantOrderId = merchantOrderId;
    this.merchantCustomerId = merchantCustomerId;
    this.amountMinor = amountMinor;
    this.currency = currency;
    this.status = status;
  }

  public Payment(PaymentStatus paymentStatus) {
    this.status = paymentStatus;
  }

  public UUID getId() {
    return id;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public String getMerchantOrderId() {
    return merchantOrderId;
  }

  public String getMerchantCustomerId() {
    return merchantCustomerId;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setStatus(PaymentStatus status) {
    this.status = status;
  }
}
