package com.osigie.payment_gateway.repository;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i from IdempotencyKey i WHERE i.merchant.id = :merchantId AND i.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyKey> findIdempotencyForUpdate(
            String idempotencyKey, UUID merchantId);

    Optional<IdempotencyKey> findByMerchantIdAndIdempotencyKey(
            UUID merchantId,
            String key
    );
}
