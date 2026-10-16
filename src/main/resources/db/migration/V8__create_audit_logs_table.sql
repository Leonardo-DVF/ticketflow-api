CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_audit_logs_event_type
        CHECK (event_type IN ('PAYMENT_APPROVED')),
    CONSTRAINT ck_audit_logs_amount_positive
        CHECK (amount > 0),
    CONSTRAINT uk_audit_logs_event
        UNIQUE (event_type, aggregate_id)
);

CREATE INDEX idx_audit_logs_reservation
    ON audit_logs (reservation_id);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs (created_at DESC);
