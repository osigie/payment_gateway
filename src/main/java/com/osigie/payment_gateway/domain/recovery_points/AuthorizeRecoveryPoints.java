package com.osigie.payment_gateway.domain.recovery_points;

public final class AuthorizeRecoveryPoints {

    private AuthorizeRecoveryPoints() {
    }

    public static final String AUTHORIZATION_CREATED = "AUTHORIZATION_CREATED";
    public static final String BANK_AUTHORIZED = "BANK_AUTHORIZED";
    public static final String BANK_AUTHORIZATION_COMPLETED = "BANK_AUTHORIZATION_COMPLETED";
    public static final String STARTED = "STARTED";
    public static final String FINISHED = "FINISHED";

}
