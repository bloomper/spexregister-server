CREATE TABLE IF NOT EXISTS activity
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    spexare_id       BIGINT                NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_ACTIVITY PRIMARY KEY (id)
);

ALTER TABLE activity
    ADD CONSTRAINT FK_ACTIVITY_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

