ALTER TABLE revinfo
    ADD COLUMN source    VARCHAR(32)  DEFAULT NULL,
    ADD COLUMN operation VARCHAR(255) DEFAULT NULL,
    ADD COLUMN comment   VARCHAR(512) DEFAULT NULL;

CREATE INDEX IX_revinfo_modified_at ON revinfo (modified_at);
CREATE INDEX IX_revinfo_modified_by ON revinfo (modified_by);
CREATE INDEX IX_revinfo_source ON revinfo (source);
