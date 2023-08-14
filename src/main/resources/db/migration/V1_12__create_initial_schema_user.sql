CREATE TABLE IF NOT EXISTS user
(
    id                          BIGINT AUTO_INCREMENT NOT NULL,
    uid                         VARCHAR(255)          NOT NULL,
    password                    VARCHAR(255)          NULL,
    state                       VARCHAR(255)          NOT NULL,
    spexare_id                  BIGINT                NULL,
    created_by                  VARCHAR(255)          NOT NULL,
    created_at                  DATETIME              NOT NULL,
    last_modified_by            VARCHAR(255)          NULL,
    last_modified_at            DATETIME              NULL,
    CONSTRAINT PK_USER PRIMARY KEY (id)
);

ALTER TABLE user
    ADD CONSTRAINT UC_USER_UID UNIQUE (uid);

CREATE INDEX IX_USER_ON_UID ON user (uid);

CREATE INDEX IX_USER_ON_SPEXARE_ID ON user (spexare_id);

ALTER TABLE user
    ADD CONSTRAINT FK_USER_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);
