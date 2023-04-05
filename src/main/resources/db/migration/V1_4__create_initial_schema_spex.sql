CREATE TABLE IF NOT EXISTS spex
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    year             VARCHAR(4)            NOT NULL,
    parent_id        BIGINT                NULL,
    details_id       BIGINT                NOT NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_SPEX PRIMARY KEY (id)
);

ALTER TABLE spex
    ADD CONSTRAINT FK_SPEX_ON_DETAILS FOREIGN KEY (details_id) REFERENCES spex_details (id);

ALTER TABLE spex
    ADD CONSTRAINT FK_SPEX_ON_PARENT FOREIGN KEY (parent_id) REFERENCES spex (id);

CREATE INDEX IDX_SPEX_ON_YEAR ON spex (year);
