CREATE TABLE IF NOT EXISTS consent
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    value            BIT                   NOT NULL,
    type_id          VARCHAR(255)          NOT NULL,
    spexare_id       BIGINT                NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_CONSENT PRIMARY KEY (id)
);

ALTER TABLE consent
    ADD CONSTRAINT FK_CONSENT_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE consent
    ADD CONSTRAINT FK_CONSENT_ON_TYPE FOREIGN KEY (type_id) REFERENCES type (id);

CREATE INDEX IDX_CONSENT_ON_SPEXARE_ID ON consent (spexare_id);
