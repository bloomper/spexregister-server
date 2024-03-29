CREATE TABLE IF NOT EXISTS spex_details
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    title               VARCHAR(255)          NOT NULL,
    poster              MEDIUMBLOB            NULL,
    poster_content_type VARCHAR(255)          NULL,
    category_id         BIGINT                NULL,
    created_by          VARCHAR(255)          NOT NULL,
    created_at          DATETIME              NOT NULL,
    last_modified_by    VARCHAR(255)          NULL,
    last_modified_at    DATETIME              NULL,
    CONSTRAINT PK_SPEX_DETAILS PRIMARY KEY (id)
);

ALTER TABLE spex_details
    ADD CONSTRAINT FK_SPEX_DETAILS_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES spex_category (id);

CREATE INDEX IX_SPEX_DETAILS_ON_TITLE ON spex_details (title);
