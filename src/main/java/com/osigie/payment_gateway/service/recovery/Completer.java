package com.osigie.payment_gateway.service.recovery;

import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.bank.recovery_points.Constants;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import com.osigie.payment_gateway.service.recovery.handler.RecoveryHandler;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class Completer {

    private static final Logger LOG = LoggerFactory.getLogger(Completer.class);
    private static final int BATCH_SIZE = 1000;
    private static final Duration RETRY_AFTER = Duration.ofMinutes(2);


    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final Map<Operation, RecoveryHandler> handlers;

    public Completer(IdempotencyKeyRepository idempotencyKeyRepository, List<RecoveryHandler> handlers) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.handlers = handlers.stream().collect(Collectors.toMap(RecoveryHandler::operation, Function.identity()));
    }


    @Scheduled(fixedDelay = 15L, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void reap() {
        OffsetDateTime timeout =
                OffsetDateTime.now(ZoneOffset.UTC).minus(RETRY_AFTER);

        while (true) {

            List<IdempotencyKey> keys =
                    idempotencyKeyRepository.findIncompleteKeys(
                            timeout,
                            Constants.FINISHED,
                            PageRequest.of(0, BATCH_SIZE)
                    );

            if (keys.isEmpty()) {
                return;
            }

            for (IdempotencyKey key : keys) {

                try {
                    RecoveryHandler handler =
                            handlers.get(key.getOperation());
                    if (handler == null) {
                        throw new IllegalStateException(
                                "No handler for "
                                        + key.getOperation()
                        );
                    }
                    handler.resume(key);

                } catch (Exception ex) {
                    LOG.error(
                            "Recovery failed for {}",
                            key.getId(),
                            ex
                    );
                }
            }
        }
    }

}
