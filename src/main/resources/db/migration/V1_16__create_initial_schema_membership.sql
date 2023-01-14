CREATE TABLE IF NOT EXISTS membership
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    "year"     VARCHAR(4)            NOT NULL,
    type       VARCHAR(255)          NOT NULL,
    spexare_id BIGINT                NULL,
    CONSTRAINT pk_membership PRIMARY KEY (id)
);

ALTER TABLE membership
    ADD CONSTRAINT FK_MEMBERSHIP_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

CREATE INDEX IDX_MEMBERSHIP_ON_SPEXARE_ID ON membership (spexare_id);
