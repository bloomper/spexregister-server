CREATE TABLE IF NOT EXISTS user_details
(
    user_id    BIGINT NOT NULL,
    spexare_id BIGINT NULL,
    CONSTRAINT pk_user_details PRIMARY KEY (user_id)
);

ALTER TABLE user_details
    ADD CONSTRAINT uc_user_details_spexare UNIQUE (spexare_id);

ALTER TABLE user_details
    ADD CONSTRAINT FK_USER_DETAILS_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE user_details
    ADD CONSTRAINT FK_USER_DETAILS_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);
