CREATE TABLE IF NOT EXISTS type
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    created_by             VARCHAR(50)           NOT NULL,
    created_at             DATETIME              NOT NULL,
    last_modified_by       VARCHAR(50)           NULL,
    last_modified_at       DATETIME              NULL,
    value                  VARCHAR(255)          NOT NULL,
    type                   VARCHAR(255)          NOT NULL,
    labels                 JSON                  NOT NULL,
    CONSTRAINT pk_type PRIMARY KEY (id)
);
