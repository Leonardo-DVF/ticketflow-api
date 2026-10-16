CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    code UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tickets_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_tickets_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_tickets_seat
        FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT uk_tickets_code UNIQUE (code),
    CONSTRAINT uk_tickets_seat UNIQUE (seat_id)
);

CREATE INDEX idx_tickets_user_created_at
    ON tickets (user_id, created_at DESC);

CREATE INDEX idx_tickets_reservation
    ON tickets (reservation_id);
