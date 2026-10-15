CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservations_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT ck_reservations_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_reservations_total_amount_positive
        CHECK (total_amount > 0),
    CONSTRAINT ck_reservations_expiration
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_reservations_user_created_at
    ON reservations (user_id, created_at DESC);

CREATE INDEX idx_reservations_status_expires_at
    ON reservations (status, expires_at);

CREATE TABLE reservation_items (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,

    CONSTRAINT fk_reservation_items_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_reservation_items_seat
        FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT uk_reservation_items_reservation_seat
        UNIQUE (reservation_id, seat_id),
    CONSTRAINT ck_reservation_items_unit_price_positive
        CHECK (unit_price > 0)
);

CREATE INDEX idx_reservation_items_seat
    ON reservation_items (seat_id);
