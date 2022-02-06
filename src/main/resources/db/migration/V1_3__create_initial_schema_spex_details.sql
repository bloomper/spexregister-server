CREATE TABLE IF NOT EXISTS spex_details
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    created_by          VARCHAR(50)           NOT NULL,
    created_date        BIGINT                NOT NULL,
    last_modified_by    VARCHAR(50)           NULL,
    last_modified_date  BIGINT                NULL,
    title               VARCHAR(255)          NOT NULL,
    poster              BLOB                  NULL,
    poster_content_type VARCHAR(255)          NULL,
    CONSTRAINT pk_spex_details PRIMARY KEY (id)
);

CREATE INDEX IDX_SPEX_DETAILS_ON_TITLE ON spex_details (title);