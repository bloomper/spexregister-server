CREATE TABLE IF NOT EXISTS user
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    uid              VARCHAR(255)          NOT NULL,
    first_name       VARCHAR(50)           NULL,
    last_name        VARCHAR(50)           NULL,
    password         VARCHAR(254)          NULL,
    activated        BIT                   NOT NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_USER PRIMARY KEY (id)
);

ALTER TABLE user
    ADD CONSTRAINT uc_user_uid UNIQUE (uid);

CREATE INDEX IDX_USER_ON_UID ON user (uid);
