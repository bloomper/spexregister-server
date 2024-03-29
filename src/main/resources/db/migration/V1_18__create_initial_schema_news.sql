CREATE TABLE IF NOT EXISTS news
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    visible_from     date                  NULL,
    visible_to       date                  NULL,
    subject          VARCHAR(255)          NOT NULL,
    text             LONGTEXT              NOT NULL,
    published        BIT                   NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_NEWS PRIMARY KEY (id)
);
