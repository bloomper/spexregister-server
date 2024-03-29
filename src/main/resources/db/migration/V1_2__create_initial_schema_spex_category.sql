CREATE TABLE IF NOT EXISTS spex_category
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    name              VARCHAR(255)          NOT NULL,
    first_year        VARCHAR(4)            NOT NULL,
    logo              MEDIUMBLOB            NULL,
    logo_content_type VARCHAR(255)          NULL,
    created_by        VARCHAR(255)          NOT NULL,
    created_at        DATETIME              NOT NULL,
    last_modified_by  VARCHAR(255)          NULL,
    last_modified_at  DATETIME              NULL,
    CONSTRAINT PK_SPEX_CATEGORY PRIMARY KEY (id)
);

CREATE INDEX IX_SPEX_CATEGORY_ON_NAME ON spex_category (name);
