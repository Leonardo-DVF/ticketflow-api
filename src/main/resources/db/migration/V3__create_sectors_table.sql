CREATE TABLE sectors (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,

    CONSTRAINT fk_sectors_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT ck_sectors_price_positive
        CHECK (price > 0)
);

CREATE UNIQUE INDEX uk_sectors_event_name
    ON sectors (event_id, LOWER(name));
