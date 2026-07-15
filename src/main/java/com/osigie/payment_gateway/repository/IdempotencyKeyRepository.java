package com.osigie.payment_gateway.repository;

import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"payment", "merchant"})
    @Query("""
            SELECT i from IdempotencyKey i WHERE i.merchant.id = :merchantId AND i.idempotencyKey = :idempotencyKey AND i.requestPath = :requestPath
            """)
    Optional<IdempotencyKey> findIdempotencyForUpdate(
            String idempotencyKey, UUID merchantId, String requestPath);


    Optional<IdempotencyKey> findByMerchantIdAndIdempotencyKeyAndRequestPath(UUID merchantId, String idempotencyKey, String requestPath);


    @Query("""
            SELECT r.id FROM IdempotencyKey r WHERE r.createdAt < :cutoff
            """)
    List<UUID> findExpiredKeys(@Param("cutoff") OffsetDateTime cutoff, Pageable pageable);
}

