CREATE TABLE IF NOT EXISTS activity
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    spexare_id       BIGINT                NULL,
    CONSTRAINT pk_activity PRIMARY KEY (id)
);

ALTER TABLE activity
    ADD CONSTRAINT FK_ACTIVITY_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

