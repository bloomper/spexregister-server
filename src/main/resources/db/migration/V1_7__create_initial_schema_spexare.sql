CREATE TABLE IF NOT EXISTS spexare
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    first_name             VARCHAR(255)          NOT NULL,
    last_name              VARCHAR(255)          NOT NULL,
    nick_name              VARCHAR(255)          NULL,
    social_security_number VARCHAR(255)          NULL,
    graduation             VARCHAR(255)          NULL,
    comment                LONGTEXT              NULL,
    image                  MEDIUMBLOB            NULL,
    image_content_type     VARCHAR(255)          NULL,
    partner_id             BIGINT                NULL,
    created_by             VARCHAR(255)          NOT NULL,
    created_at             DATETIME              NOT NULL,
    last_modified_by       VARCHAR(255)          NULL,
    last_modified_at       DATETIME              NULL,
    CONSTRAINT PK_SPEXARE PRIMARY KEY (id)
);

ALTER TABLE spexare
    ADD CONSTRAINT FK_SPEXARE_ON_PARTNER FOREIGN KEY (partner_id) REFERENCES spexare (id);

CREATE INDEX IX_SPEXARE_ON_LAST_NAME ON spexare (last_name);

CREATE INDEX IX_SPEXARE_ON_FIRST_NAME ON spexare (first_name);

CREATE INDEX IX_SPEXARE_ON_NICK_NAME ON spexare (nick_name);
