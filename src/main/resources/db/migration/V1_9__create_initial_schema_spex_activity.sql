CREATE TABLE IF NOT EXISTS spex_activity
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    activity_id BIGINT                NOT NULL,
    spex_id     BIGINT                NOT NULL,
    CONSTRAINT pk_spex_activity PRIMARY KEY (id)
);

ALTER TABLE spex_activity
    ADD CONSTRAINT FK_SPEX_ACTIVITY_ON_ACTIVITY FOREIGN KEY (activity_id) REFERENCES activity (id);

ALTER TABLE spex_activity
    ADD CONSTRAINT FK_SPEX_ACTIVITY_ON_SPEX FOREIGN KEY (spex_id) REFERENCES spex (id);

