CREATE TABLE IF NOT EXISTS consent
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    value            BIT                   NOT NULL,
    type_id          BIGINT                NOT NULL,
    spexare_id       BIGINT                NULL,
    CONSTRAINT pk_consent PRIMARY KEY (id)
);

ALTER TABLE consent
    ADD CONSTRAINT FK_CONSENT_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE consent
    ADD CONSTRAINT FK_CONSENT_ON_TYPE FOREIGN KEY (type_id) REFERENCES type (id);

CREATE INDEX IDX_CONSENT_ON_SPEXARE_ID ON consent (spexare_id);
