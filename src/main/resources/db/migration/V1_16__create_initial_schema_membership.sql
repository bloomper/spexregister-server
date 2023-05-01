CREATE TABLE IF NOT EXISTS membership
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    year             VARCHAR(4)            NOT NULL,
    type_id          VARCHAR(255)          NOT NULL,
    spexare_id       BIGINT                NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_MEMBERSHIP PRIMARY KEY (id)
);

ALTER TABLE membership
    ADD CONSTRAINT FK_MEMBERSHIP_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE membership
    ADD CONSTRAINT FK_MEMBERSHIP_ON_TYPE FOREIGN KEY (type_id) REFERENCES type (id);

CREATE INDEX IX_MEMBERSHIP_ON_SPEXARE_ID ON membership (spexare_id);
