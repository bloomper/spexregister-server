CREATE TABLE IF NOT EXISTS authority
(
    id                          VARCHAR(255)          NOT NULL,
    labels                      JSON                  NOT NULL,
    created_by                  VARCHAR(255)          NOT NULL,
    created_at                  DATETIME              NOT NULL,
    last_modified_by            VARCHAR(255)          NULL,
    last_modified_at            DATETIME              NULL,
    CONSTRAINT PK_AUTHORITY PRIMARY KEY (id)
);
