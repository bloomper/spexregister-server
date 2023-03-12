CREATE TABLE IF NOT EXISTS spexare
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    created_by             VARCHAR(50)           NOT NULL,
    created_at             DATETIME              NOT NULL,
    last_modified_by       VARCHAR(50)           NULL,
    last_modified_at       DATETIME              NULL,
    first_name             VARCHAR(255)          NOT NULL,
    last_name              VARCHAR(255)          NOT NULL,
    nick_name              VARCHAR(255)          NULL,
    birth_date             date                  NULL,
    social_security_number VARCHAR(4)            NULL,
    graduation             VARCHAR(255)          NULL,
    comment                LONGTEXT              NULL,
    image                  BLOB                  NULL,
    image_content_type     VARCHAR(255)          NULL,
    partner_id             BIGINT                NULL,
    CONSTRAINT pk_spexare PRIMARY KEY (id)
);

ALTER TABLE spexare
    ADD CONSTRAINT FK_SPEXARE_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES spexare (id);

CREATE INDEX IDX_SPEXARE_ON_LAST_NAME ON spexare (last_name);

CREATE INDEX IDX_SPEXARE_ON_FIRST_NAME ON spexare (first_name);

CREATE INDEX IDX_SPEXARE_ON_NICK_NAME ON spexare (nick_name);
