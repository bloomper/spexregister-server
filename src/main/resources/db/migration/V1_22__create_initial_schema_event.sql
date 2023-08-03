CREATE TABLE IF NOT EXISTS event
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    event            VARCHAR(255)          NOT NULL,
    source           VARCHAR(255)          NOT NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    CONSTRAINT PK_EVENT PRIMARY KEY (id)
);

CREATE INDEX IX_EVENT_ON_SOURCE ON event (source);
