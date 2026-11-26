-- FGV memberships move to Medlemskap (spexregister-spec, docs/base-data-and-memberships.md, section 5). The copy here is
-- written only by the sync, and a membership may be for life from its year onwards.
ALTER TABLE membership
    ADD COLUMN lifetime BIT NOT NULL DEFAULT 0;
ALTER TABLE membership_audit
    ADD COLUMN lifetime BIT NULL;

-- Medlemskap's change feed is a second source beside Spexipedia's
INSERT INTO sync_source (name) VALUES ('medlemskap');

-- Spexare that were deleted here and have not yet been reported to Medlemskap (section 5.5); sent at once after
-- the deletion and retried by a job until it works
CREATE TABLE medlemskap_notification
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    spexare_id CHAR(36)    NOT NULL,
    created_at DATETIME(6) NOT NULL,
    attempts   INT         NOT NULL DEFAULT 0,
    CONSTRAINT PK_MEDLEMSKAP_NOTIFICATION PRIMARY KEY (id)
);
