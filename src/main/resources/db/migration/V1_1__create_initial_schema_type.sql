CREATE TABLE IF NOT EXISTS type
(
    id                     VARCHAR(255)          NOT NULL,
    type                   VARCHAR(255)          NOT NULL,
    labels                 JSON                  NOT NULL,
    created_by             VARCHAR(255)          NOT NULL,
    created_at             DATETIME              NOT NULL,
    last_modified_by       VARCHAR(255)          NULL,
    last_modified_at       DATETIME              NULL,
    CONSTRAINT PK_TYPE PRIMARY KEY (id)
);
