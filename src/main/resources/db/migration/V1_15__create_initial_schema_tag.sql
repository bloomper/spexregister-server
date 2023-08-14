CREATE TABLE IF NOT EXISTS tag
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    name             VARCHAR(255)          NOT NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_TAG PRIMARY KEY (id)
);

CREATE INDEX IX_TAG_ON_NAME ON tag (name);
