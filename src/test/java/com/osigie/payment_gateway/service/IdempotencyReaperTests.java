package com.osigie.payment_gateway.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import com.osigie.payment_gateway.service.impl.IdempotencyReaperImpl;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class IdempotencyReaperTests {

  private static final int BATCH_SIZE = 1000;

  @Mock private IdempotencyKeyRepository idempotencyKeyRepository;

  @InjectMocks private IdempotencyReaperImpl idempotencyReaper;

  @Test
  void reap_withExpiredKeys_shouldDeleteThem() {
    var batch = IntStream.range(0, BATCH_SIZE).mapToObj(i -> UUID.randomUUID()).toList();
    when(idempotencyKeyRepository.findExpiredKeys(any(), any(PageRequest.class)))
        .thenReturn(batch)
        .thenReturn(List.of());

    idempotencyReaper.reap();

    verify(idempotencyKeyRepository).deleteAllByIdInBatch(batch);
    verify(idempotencyKeyRepository, times(2)).findExpiredKeys(any(), any(PageRequest.class));
  }

  @Test
  void reap_withNoExpiredKeys_shouldDoNothing() {
    when(idempotencyKeyRepository.findExpiredKeys(any(), any(PageRequest.class)))
        .thenReturn(List.of());

    idempotencyReaper.reap();

    verify(idempotencyKeyRepository, never()).deleteAllByIdInBatch(any());
  }

  @Test
  void reap_withMoreThanBatchSize_shouldProcessInBatches() {
    var batch1 = IntStream.range(0, BATCH_SIZE).mapToObj(i -> UUID.randomUUID()).toList();
    var batch2 = List.of(UUID.randomUUID());

    when(idempotencyKeyRepository.findExpiredKeys(any(), any(PageRequest.class)))
        .thenReturn(batch1)
        .thenReturn(batch2);

    idempotencyReaper.reap();

    verify(idempotencyKeyRepository).deleteAllByIdInBatch(batch1);
    verify(idempotencyKeyRepository).deleteAllByIdInBatch(batch2);
  }
}
