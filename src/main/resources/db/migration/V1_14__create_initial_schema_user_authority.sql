CREATE TABLE IF NOT EXISTS user_authority
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    user_id          BIGINT                NOT NULL,
    authority_id     BIGINT                NOT NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_USER_AUTHORITY PRIMARY KEY (id)
);

ALTER TABLE user_authority
    ADD CONSTRAINT FK_USER_AUTHORITY_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE user_authority
    ADD CONSTRAINT FK_USER_AUTHORITY_ON_AUTHORITY FOREIGN KEY (authority_id) REFERENCES authority (id);
