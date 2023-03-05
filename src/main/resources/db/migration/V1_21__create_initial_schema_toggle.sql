CREATE TABLE IF NOT EXISTS toggle
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    value            BIT                   NOT NULL,
    type_id          VARCHAR(255)          NOT NULL,
    spexare_id       BIGINT                NULL,
    CONSTRAINT pk_toggle PRIMARY KEY (id)
);

ALTER TABLE toggle
    ADD CONSTRAINT FK_TOGGLE_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE toggle
    ADD CONSTRAINT FK_TOGGLE_ON_TYPE FOREIGN KEY (type_id) REFERENCES type (id);

CREATE INDEX IDX_TOGGLE_ON_SPEXARE_ID ON toggle (spexare_id);
