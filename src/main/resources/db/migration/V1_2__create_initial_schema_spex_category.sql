CREATE TABLE IF NOT EXISTS spex_category
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_by        VARCHAR(50)           NOT NULL,
    created_at        BIGINT                NOT NULL,
    last_modified_by  VARCHAR(50)           NULL,
    last_modified_at  BIGINT                NULL,
    name              VARCHAR(255)          NOT NULL,
    first_year        VARCHAR(4)            NOT NULL,
    logo              BLOB                  NULL,
    logo_content_type VARCHAR(255)          NULL,
    CONSTRAINT pk_spex_category PRIMARY KEY (id)
);

CREATE INDEX IDX_SPEX_CATEGORY_ON_NAME ON spex_category (name);
