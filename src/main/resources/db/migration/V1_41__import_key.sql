-- The import API (spexregister-spec, docs/migration.md, 3): what each source key made, so that importing it again
-- updates that record instead of making another, and leaves it alone once someone has changed it.
CREATE TABLE import_key
(
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    source_key     VARCHAR(1000) NOT NULL,
    -- SHA-256 of the source key, for the unique index (source keys may be long)
    source_hash    CHAR(64)      NOT NULL,
    record_kind    VARCHAR(30)   NOT NULL,
    record_id      CHAR(36)      NOT NULL,
    -- SHA-256 of the record as last imported, to tell UNCHANGED from UPDATED
    imported_hash  CHAR(64)      NOT NULL,
    -- A fingerprint of what the import wrote; a different one later means someone changed it (KEPT)
    record_version BIGINT        NULL,
    imported_by    VARCHAR(64)   NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    CONSTRAINT PK_IMPORT_KEY PRIMARY KEY (id),
    CONSTRAINT UC_IMPORT_KEY_SOURCE UNIQUE (source_hash),
    INDEX IX_IMPORT_KEY_RECORD (record_kind, record_id),
    INDEX IX_IMPORT_KEY_PREFIX (source_key(255))
);
