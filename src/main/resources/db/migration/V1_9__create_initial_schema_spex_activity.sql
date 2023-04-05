CREATE TABLE IF NOT EXISTS spex_activity
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    activity_id      BIGINT                NOT NULL,
    spex_id          BIGINT                NOT NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_SPEX_ACTIVITY PRIMARY KEY (id)
);

ALTER TABLE spex_activity
    ADD CONSTRAINT FK_SPEX_ACTIVITY_ON_ACTIVITY FOREIGN KEY (activity_id) REFERENCES activity (id);

ALTER TABLE spex_activity
    ADD CONSTRAINT FK_SPEX_ACTIVITY_ON_SPEX FOREIGN KEY (spex_id) REFERENCES spex (id);

