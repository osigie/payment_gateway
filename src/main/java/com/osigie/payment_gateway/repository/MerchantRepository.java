package com.osigie.payment_gateway.repository;

import com.osigie.payment_gateway.domain.entity.Merchant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
  Optional<Merchant> findByApiKey(String apiKey);
}
