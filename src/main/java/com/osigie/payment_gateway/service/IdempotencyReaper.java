package com.osigie.payment_gateway.service.recovery;

import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyReaper {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyReaper.class);
    private static final int BATCH_SIZE = 1000;
    private static final Duration REAP_TIMEOUT = Duration.ofHours(24);

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyReaper(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Scheduled(fixedDelay = 5L, timeUnit = TimeUnit.HOURS, initialDelay = 1L)
    @Transactional
    public void reap() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minus(REAP_TIMEOUT);
        int reaped = 0;
        do {

            List<UUID> keys = idempotencyKeyRepository.findExpiredKeys(cutoff, PageRequest.of(0, BATCH_SIZE));

            if (!keys.isEmpty()) {
                idempotencyKeyRepository.deleteAllByIdInBatch(keys);
            }
            reaped = keys.size();

            LOG.info("Reaped {} keys", reaped);
        } while (reaped == BATCH_SIZE);
    }


}
