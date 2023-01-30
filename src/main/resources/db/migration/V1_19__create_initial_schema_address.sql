CREATE TABLE IF NOT EXISTS address
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    created_by             VARCHAR(50)           NOT NULL,
    created_at             DATETIME              NOT NULL,
    last_modified_by       VARCHAR(50)           NULL,
    last_modified_at       DATETIME              NULL,
    street_address         VARCHAR(255)          NULL,
    postal_code            VARCHAR(255)          NULL,
    city                   VARCHAR(255)          NULL,
    country                VARCHAR(255)          NULL,
    phone                  VARCHAR(255)          NULL,
    mobile_phone           VARCHAR(255)          NULL,
    email_address          VARCHAR(255)          NULL,
    type_id                BIGINT                NOT NULL,
    spexare_id             BIGINT                NULL,
    CONSTRAINT pk_address PRIMARY KEY (id)
);

ALTER TABLE address
    ADD CONSTRAINT FK_ADDRESS_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE address
    ADD CONSTRAINT FK_ADDRESS_ON_TYPE FOREIGN KEY (type_id) REFERENCES type (id);

CREATE INDEX IDX_ADDRESS_ON_SPEXARE_ID ON address (spexare_id);
