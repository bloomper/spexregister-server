CREATE TABLE IF NOT EXISTS user
(
    id                          BIGINT AUTO_INCREMENT NOT NULL,
    external_id                 VARCHAR(255)          NOT NULL,
    state_id                    VARCHAR(255)          DEFAULT 'PENDING',
    spexare_id                  BIGINT                NULL,
    created_by                  VARCHAR(255)          NOT NULL,
    created_at                  DATETIME              NOT NULL,
    last_modified_by            VARCHAR(255)          NULL,
    last_modified_at            DATETIME              NULL,
    CONSTRAINT PK_USER PRIMARY KEY (id)
);

ALTER TABLE user
    ADD CONSTRAINT UC_USER_EXTERNAL_ID UNIQUE (external_id);

CREATE INDEX IX_USER_ON_EXTERNAL_ID ON user (external_id);

CREATE INDEX IX_USER_ON_SPEXARE_ID ON user (spexare_id);

ALTER TABLE user
    ADD CONSTRAINT FK_USER_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE user
    ADD CONSTRAINT FK_USER_ON_STATE FOREIGN KEY (state_id) REFERENCES state (id);
