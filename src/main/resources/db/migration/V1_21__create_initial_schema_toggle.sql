CREATE TABLE IF NOT EXISTS toggle
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    value            BIT                   NOT NULL,
    type_id          VARCHAR(255)          NOT NULL,
    spexare_id       BIGINT                NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    CONSTRAINT PK_TOGGLE PRIMARY KEY (id)
);

ALTER TABLE toggle
    ADD CONSTRAINT FK_TOGGLE_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE toggle
    ADD CONSTRAINT FK_TOGGLE_ON_TYPE FOREIGN KEY (type_id) REFERENCES type (id);

CREATE INDEX IX_TOGGLE_ON_SPEXARE_ID ON toggle (spexare_id);
