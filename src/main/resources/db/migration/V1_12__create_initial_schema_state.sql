CREATE TABLE IF NOT EXISTS state
(
    id                          VARCHAR(255)          NOT NULL,
    labels                      JSON                  NOT NULL,
    initial                     BIT                   DEFAULT 0,
    enabled                     BIT                   DEFAULT 0,
    created_by                  VARCHAR(255)          NOT NULL,
    created_at                  DATETIME              NOT NULL,
    last_modified_by            VARCHAR(255)          NULL,
    last_modified_at            DATETIME              NULL,
    version                     BIGINT                NULL,
    CONSTRAINT PK_STATE PRIMARY KEY (id)
);
