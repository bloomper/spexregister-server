CREATE TABLE IF NOT EXISTS tag
(
    id   BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255)          NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id)
);

CREATE INDEX IDX_TAG_ON_NAME ON tag (name);