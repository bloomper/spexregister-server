CREATE TABLE IF NOT EXISTS "user"
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    uid              VARCHAR(254)          NOT NULL,
    first_name       VARCHAR(50)           NULL,
    last_name        VARCHAR(50)           NULL,
    password         VARCHAR(254)          NULL,
    activated        BIT                   NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (id)
);

ALTER TABLE "user"
    ADD CONSTRAINT uc_user_uid UNIQUE (uid);

CREATE INDEX IDX_USER_ON_UID ON "user" (uid);
