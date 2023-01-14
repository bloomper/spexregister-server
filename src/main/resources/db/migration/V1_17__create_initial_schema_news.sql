CREATE TABLE IF NOT EXISTS news
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    publication_date date                  NOT NULL,
    subject          VARCHAR(255)          NOT NULL,
    text             LONGTEXT              NOT NULL,
    published        BIT                   NULL,
    CONSTRAINT pk_news PRIMARY KEY (id)
);

CREATE INDEX IDX_NEWS_ON_PUBLICATION_DATE ON news (publication_date);
