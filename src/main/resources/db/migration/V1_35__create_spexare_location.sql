CREATE TABLE IF NOT EXISTS spexare_location
(
    spexare_id  BIGINT        NOT NULL,
    query       VARCHAR(1024) NOT NULL,
    latitude    DOUBLE        NULL,
    longitude   DOUBLE        NULL,
    geocoded_at DATETIME(6)   NOT NULL,
    CONSTRAINT PK_SPEXARE_LOCATION PRIMARY KEY (spexare_id)
);

ALTER TABLE spexare_location
    ADD CONSTRAINT FK_SPEXARE_LOCATION_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id) ON DELETE CASCADE;
