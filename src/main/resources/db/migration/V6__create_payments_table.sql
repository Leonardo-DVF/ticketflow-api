CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT ck_payments_amount_positive
        CHECK (amount > 0),
    CONSTRAINT ck_payments_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_payments_reservation_created_at
    ON payments (reservation_id, created_at DESC);

CREATE UNIQUE INDEX uk_payments_one_approved_per_reservation
    ON payments (reservation_id)
    WHERE status = 'APPROVED';
