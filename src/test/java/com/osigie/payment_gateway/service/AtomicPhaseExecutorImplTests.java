package com.osigie.payment_gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.osigie.payment_gateway.domain.ErrorCode;
import com.osigie.payment_gateway.domain.Operation;
import com.osigie.payment_gateway.domain.PhaseResult;
import com.osigie.payment_gateway.domain.bank.recovery_points.AuthorizeRecoveryPoints;
import com.osigie.payment_gateway.domain.entity.IdempotencyKey;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.dto.Result;
import com.osigie.payment_gateway.service.impl.AtomicPhaseExecutorImpl;
import com.osigie.payment_gateway.util.TestDataFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AtomicPhaseExecutorImplTests {

  @Mock private IdempotencyKeyService idempotencyKeyService;

  private AtomicPhaseExecutorImpl executor;
  private Merchant merchant;
  private IdempotencyKey idempotencyKey;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();

    executor = new AtomicPhaseExecutorImpl(objectMapper, idempotencyKeyService);
    merchant = TestDataFactory.createTestMerchant();
    idempotencyKey =
        TestDataFactory.createIdempotencyKey(merchant, null, AuthorizeRecoveryPoints.STARTED);
  }

  @Test
  void execute_withRecoveryPointResult_shouldUpdateRecoveryPoint() {
    when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
        .thenReturn(Optional.of(idempotencyKey));
    doNothing().when(idempotencyKeyService).update(any());

    executor.execute(
        merchant.getId(),
        "ikey-1",
        Operation.PAYMENT_AUTHORIZE,
        key -> key,
        key -> new PhaseResult.RecoveryPoint(AuthorizeRecoveryPoints.AUTHORIZATION_CREATED));

    assertThat(idempotencyKey.getRecoveryPoint())
        .isEqualTo(AuthorizeRecoveryPoints.AUTHORIZATION_CREATED);
    verify(idempotencyKeyService).update(idempotencyKey);
  }

  @Test
  void execute_withResponseResult_shouldSetFinishedAndResponseBody() {
    when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
        .thenReturn(Optional.of(idempotencyKey));
    doNothing().when(idempotencyKeyService).update(any());

    executor.execute(
        merchant.getId(),
        "ikey-2",
        Operation.PAYMENT_AUTHORIZE,
        key -> key,
        key -> new PhaseResult.Response<>(Result.success("done")));

    assertThat(idempotencyKey.getRecoveryPoint()).isEqualTo(AuthorizeRecoveryPoints.FINISHED);
    assertThat(idempotencyKey.getResponseBody()).isNotNull();
    assertThat(idempotencyKey.getResponseStatus()).isEqualTo(200);
    verify(idempotencyKeyService).update(idempotencyKey);
  }

  @Test
  void execute_withFailedResponse_shouldSetErrorStatus() {
    when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
        .thenReturn(Optional.of(idempotencyKey));
    doNothing().when(idempotencyKeyService).update(any());

    executor.execute(
        merchant.getId(),
        "ikey-3",
        Operation.PAYMENT_AUTHORIZE,
        key -> key,
        key -> new PhaseResult.Response<>(Result.failure(ErrorCode.BAD_REQUEST, "invalid")));

    assertThat(idempotencyKey.getRecoveryPoint()).isEqualTo(AuthorizeRecoveryPoints.FINISHED);
    assertThat(idempotencyKey.getResponseBody()).isNotNull();
    assertThat(idempotencyKey.getResponseStatus()).isEqualTo(400);
    verify(idempotencyKeyService).update(idempotencyKey);
  }

  @Test
  void execute_withNoOpResult_shouldNotUpdateKey() {
    when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
        .thenReturn(Optional.of(idempotencyKey));
    doNothing().when(idempotencyKeyService).update(any());

    executor.execute(
        merchant.getId(),
        "ikey-4",
        Operation.PAYMENT_AUTHORIZE,
        key -> key,
        key -> new PhaseResult.NoOp());

    assertThat(idempotencyKey.getRecoveryPoint()).isEqualTo(AuthorizeRecoveryPoints.STARTED);
    assertThat(idempotencyKey.getResponseBody()).isNull();
    verify(idempotencyKeyService).update(idempotencyKey);
  }

  @Test
  void execute_withKeyNotFound_shouldThrowIllegalStateException() {
    when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                executor.execute(
                    merchant.getId(),
                    "nonexistent",
                    Operation.PAYMENT_AUTHORIZE,
                    key -> key,
                    key -> new PhaseResult.NoOp()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Idempotency key not found");
  }

  @Test
  void execute_simpleOverload_shouldDelegate() {
    when(idempotencyKeyService.findIdempotencyForUpdate(any(), any(), any()))
        .thenReturn(Optional.of(idempotencyKey));
    doNothing().when(idempotencyKeyService).update(any());

    executor.execute(
        merchant.getId(),
        "ikey-5",
        Operation.PAYMENT_AUTHORIZE,
        key -> new PhaseResult.RecoveryPoint(AuthorizeRecoveryPoints.AUTHORIZATION_CREATED));

    assertThat(idempotencyKey.getRecoveryPoint())
        .isEqualTo(AuthorizeRecoveryPoints.AUTHORIZATION_CREATED);
    verify(idempotencyKeyService).update(idempotencyKey);
  }
}
