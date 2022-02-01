CREATE TABLE IF NOT EXISTS tagging
(
    tag_id     BIGINT NOT NULL,
    spexare_id BIGINT NOT NULL
);

ALTER TABLE tagging
    ADD CONSTRAINT FK_TAGGING_ON_TAG FOREIGN KEY (tag_id) REFERENCES tag (id);

ALTER TABLE tagging
    ADD CONSTRAINT FK_SPEXARE_ON_TAG FOREIGN KEY (spexare_id) REFERENCES spexare (id);

CREATE INDEX IDX_TAGGING_ON_TAG_ID ON tagging (tag_id);

CREATE INDEX IDX_TAGGING_ON_SPEXARE_ID ON tagging (spexare_id);

