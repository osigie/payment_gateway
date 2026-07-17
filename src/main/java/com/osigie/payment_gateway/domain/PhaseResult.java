package com.osigie.payment_gateway.domain;

import com.osigie.payment_gateway.dto.Result;

public sealed interface PhaseResult
    permits PhaseResult.NoOp, PhaseResult.RecoveryPoint, PhaseResult.Response {
  record NoOp() implements PhaseResult {}

  record RecoveryPoint(String name) implements PhaseResult {}

  record Response<T>(Result<T> result) implements PhaseResult {}
}
