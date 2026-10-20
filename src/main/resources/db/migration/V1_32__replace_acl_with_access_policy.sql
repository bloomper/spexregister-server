CREATE TABLE IF NOT EXISTS permission_grant
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    user_id          BIGINT                NULL,
    role             VARCHAR(16)           NULL,
    permission       VARCHAR(16)           NOT NULL,
    scope_type       VARCHAR(32)           NOT NULL,
    spexare_id       BIGINT                NULL,
    spex_id          BIGINT                NULL,
    valid_until      DATE                  NULL,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    version          BIGINT                NULL,
    CONSTRAINT PK_PERMISSION_GRANT PRIMARY KEY (id),
    CONSTRAINT CK_PERMISSION_GRANT_GRANTEE CHECK ((user_id IS NULL) <> (role IS NULL)),
    CONSTRAINT CK_PERMISSION_GRANT_SCOPE CHECK (
        (scope_type = 'SPEXARE' AND spexare_id IS NOT NULL AND spex_id IS NULL)
            OR (scope_type IN ('SPEX_PARTICIPANTS', 'SPEX') AND spex_id IS NOT NULL AND spexare_id IS NULL))
);

ALTER TABLE permission_grant
    ADD CONSTRAINT FK_PERMISSION_GRANT_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE permission_grant
    ADD CONSTRAINT FK_PERMISSION_GRANT_ON_SPEXARE FOREIGN KEY (spexare_id) REFERENCES spexare (id);

ALTER TABLE permission_grant
    ADD CONSTRAINT FK_PERMISSION_GRANT_ON_SPEX FOREIGN KEY (spex_id) REFERENCES spex (id);

CREATE INDEX IX_PERMISSION_GRANT_ON_USER_ID ON permission_grant (user_id);
CREATE INDEX IX_PERMISSION_GRANT_ON_ROLE ON permission_grant (role);
CREATE INDEX IX_PERMISSION_GRANT_ON_SPEXARE_ID ON permission_grant (spexare_id);
CREATE INDEX IX_PERMISSION_GRANT_ON_SPEX_ID ON permission_grant (spex_id);

CREATE TABLE IF NOT EXISTS permission_grant_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    user_id                BIGINT                NULL,
    role                   VARCHAR(16)           NULL,
    permission             VARCHAR(16)           NULL,
    scope_type             VARCHAR(32)           NULL,
    spexare_id             BIGINT                NULL,
    spex_id                BIGINT                NULL,
    valid_until            DATE                  NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_PERMISSION_GRANT_AUDIT PRIMARY KEY (id, rev)
);

DROP TABLE IF EXISTS acl_entry;
DROP TABLE IF EXISTS acl_object_identity;
DROP TABLE IF EXISTS acl_class;
DROP TABLE IF EXISTS acl_sid;
