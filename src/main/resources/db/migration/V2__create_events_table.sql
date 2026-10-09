CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    location VARCHAR(255) NOT NULL,
    event_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_events_status
        CHECK (status IN ('DRAFT', 'AVAILABLE', 'CANCELLED', 'FINISHED'))
);

CREATE INDEX idx_events_status_event_date
    ON events (status, event_date);
