-- A change from a source's feed that keeps failing is tried a few times, then recorded here and left out, so that it
-- cannot stop the changes after it. Admins see the list.
ALTER TABLE sync_source
    ADD COLUMN failing_sequence VARCHAR(100) NULL,
    ADD COLUMN failing_attempts INT          NOT NULL DEFAULT 0;

CREATE TABLE sync_skipped
(
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    source      VARCHAR(50)   NOT NULL,
    sequence    VARCHAR(100)  NOT NULL,
    kind        VARCHAR(40)   NOT NULL,
    record_id   CHAR(36)      NOT NULL,
    change_type VARCHAR(20)   NOT NULL,
    error       VARCHAR(1000) NOT NULL,
    skipped_at  DATETIME(6)   NOT NULL,
    CONSTRAINT PK_SYNC_SKIPPED PRIMARY KEY (id),
    CONSTRAINT FK_SYNC_SKIPPED_SOURCE FOREIGN KEY (source) REFERENCES sync_source (name)
);
