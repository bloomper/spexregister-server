CREATE TABLE IF NOT EXISTS user_authority
(
    user_id          BIGINT                NOT NULL,
    authority_id     VARCHAR(255)          NOT NULL
);

ALTER TABLE user_authority
    ADD CONSTRAINT FK_USER_AUTHORITY_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE user_authority
    ADD CONSTRAINT FK_USER_AUTHORITY_ON_AUTHORITY FOREIGN KEY (authority_id) REFERENCES authority (id);
