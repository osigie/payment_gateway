CREATE TABLE merchants
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    api_key    VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE payments
(
    id                   UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    merchant_id          UUID         NOT NULL REFERENCES merchants (id),
    merchant_order_id    VARCHAR(100) NOT NULL,
    merchant_customer_id VARCHAR(100) NOT NULL,
    amount_minor         BIGINT       NOT NULL,
    currency             VARCHAR(3)      NOT NULL,
    status               VARCHAR(50)  NOT NULL, -- PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED, VOIDED
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchant_order_id ON payments (merchant_order_id);
CREATE INDEX idx_merchant_customer_id ON payments (merchant_customer_id);

CREATE TABLE idempotency_keys
(
    id                   UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    idempotency_key      VARCHAR(100) NOT NULL,
    merchant_id          UUID         NOT NULL REFERENCES merchants (id),
    payment_id           UUID REFERENCES payments (id),
    request_params       JSONB        NOT NULL,
    request_path         VARCHAR(100) NOT NULL,
    response_status      INT,
    response_body        JSONB,
    recovery_point       VARCHAR(50)  NOT NULL,
    last_run_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idempotency_keys_merchants_id_idempotency_key ON idempotency_keys (merchant_id, idempotency_key);

CREATE INDEX idx_idempotency_keys_payment_id ON idempotency_keys (payment_id);

CREATE TABLE transactions
(
    id             UUID PRIMARY KEY                       DEFAULT gen_random_uuid(),
    payment_id     UUID REFERENCES payments (id) NOT NULL,
    type           VARCHAR(50)                   NOT NULL, --AUTHORIZED, CAPTURE, REFUND, VOID
    status         VARCHAR(50)                   NOT NULL, --PENDING, SUCCESS, FAILED
    amount_minor   BIGINT                        NOT NULL,
    bank_reference VARCHAR(100)                  NOT NULL,
    created_at     TIMESTAMPTZ                   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_payment_id ON transactions (payment_id);

--SEED TEST MERCHANT
INSERT INTO merchants (id, name, api_key, created_at, updated_at)
values ('d290f1ee-6c54-4b01-90e6-d701748f0851', 'Test Merchant',
        'sk_test_merchant_1234567890',
        NOW(),
        NOW());