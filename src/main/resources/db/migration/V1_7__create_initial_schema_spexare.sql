CREATE TABLE IF NOT EXISTS spexare
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    created_by             VARCHAR(50)           NOT NULL,
    created_date           BIGINT                NOT NULL,
    last_modified_by       VARCHAR(50)           NULL,
    last_modified_date     BIGINT                NULL,
    first_name             VARCHAR(255)          NOT NULL,
    last_name              VARCHAR(255)          NOT NULL,
    nick_name              VARCHAR(255)          NULL,
    street_address         VARCHAR(255)          NULL,
    postal_code            VARCHAR(255)          NULL,
    postal_address         VARCHAR(255)          NULL,
    country                VARCHAR(255)          NULL,
    phone_home             VARCHAR(255)          NULL,
    phone_work             VARCHAR(255)          NULL,
    phone_mobile           VARCHAR(255)          NULL,
    phone_other            VARCHAR(255)          NULL,
    email_address          VARCHAR(255)          NULL,
    birth_date             date                  NULL,
    social_security_number VARCHAR(4)            NULL,
    chalmers_student       BIT(1)                NULL,
    graduation             VARCHAR(255)          NULL,
    comment                LONGTEXT              NULL,
    deceased               BIT(1)                NULL,
    publish_approval       BIT(1)                NULL,
    want_circulars         BIT(1)                NULL,
    want_email_circulars   BIT(1)                NULL,
    image                  BLOB                  NULL,
    image_content_type     VARCHAR(255)          NULL,
    spouse_id              BIGINT                NULL,
    CONSTRAINT pk_spexare PRIMARY KEY (id)
);

ALTER TABLE spexare
    ADD CONSTRAINT FK_SPEXARE_ON_SPOUSE FOREIGN KEY (spouse_id) REFERENCES spexare (id);

CREATE INDEX IDX_SPEXARE_ON_LAST_NAME ON spexare (last_name);

CREATE INDEX IDX_SPEXARE_ON_FIRST_NAME ON spexare (first_name);

CREATE INDEX IDX_SPEXARE_ON_NICK_NAME ON spexare (nick_name);
