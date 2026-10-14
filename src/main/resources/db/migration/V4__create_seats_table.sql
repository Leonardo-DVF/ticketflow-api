CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    sector_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    CONSTRAINT fk_seats_sector
        FOREIGN KEY (sector_id) REFERENCES sectors (id),
    CONSTRAINT ck_seats_status
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD'))
);

CREATE UNIQUE INDEX uk_seats_sector_code
    ON seats (sector_id, LOWER(code));

CREATE INDEX idx_seats_sector_status
    ON seats (sector_id, status);
