package com.osigie.payment_gateway.domain.bank.recovery_points;

public final class CaptureRecoveryPoints {

    private CaptureRecoveryPoints() {
    }

    public static final String STARTED = "STARTED";
    public static final String BANK_CAPTURE = "BANK_CAPTURE";
    public static final String BANK_CAPTURE_COMPLETED = "BANK_CAPTURE_COMPLETED";
    public static final String FINISHED = "FINISHED";

}
