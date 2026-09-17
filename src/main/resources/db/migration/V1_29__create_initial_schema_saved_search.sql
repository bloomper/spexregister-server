CREATE TABLE IF NOT EXISTS saved_search
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    name              VARCHAR(255)          NOT NULL,
    search_query      VARCHAR(2000)         NOT NULL,
    owner_external_id VARCHAR(255)          NOT NULL,
    created_by        VARCHAR(255)          NOT NULL,
    created_at        DATETIME              NOT NULL,
    last_modified_by  VARCHAR(255)          NULL,
    last_modified_at  DATETIME              NULL,
    version           BIGINT                NULL,
    CONSTRAINT PK_SAVED_SEARCH PRIMARY KEY (id)
);

CREATE INDEX IX_SAVED_SEARCH_ON_OWNER_EXTERNAL_ID ON saved_search (owner_external_id);

ALTER TABLE saved_search
    ADD CONSTRAINT UQ_SAVED_SEARCH_ON_OWNER_EXTERNAL_ID_AND_NAME UNIQUE (owner_external_id, name);
