-- Base data (spex categories, spex, revivals, task categories, tasks) and characters move to Spexipedia. The tables stay,
-- with their keys and the activities that point at them, and are written only by the sync from Spexipedia's change
-- feed (spexregister-spec, docs/base-data-and-memberships.md, section 3). Posters and logos are Spexipedia's public
-- images: only their addresses are kept.

ALTER TABLE spex_category
    ADD COLUMN retired  BIT          NOT NULL DEFAULT 0,
    ADD COLUMN logo_url VARCHAR(1000) NULL;
ALTER TABLE spex_category_audit
    ADD COLUMN retired  BIT          NULL,
    ADD COLUMN logo_url VARCHAR(1000) NULL;

ALTER TABLE spex
    ADD COLUMN retired    BIT           NOT NULL DEFAULT 0,
    ADD COLUMN poster_url VARCHAR(1000) NULL;
ALTER TABLE spex_audit
    ADD COLUMN retired    BIT           NULL,
    ADD COLUMN poster_url VARCHAR(1000) NULL;

ALTER TABLE task_category
    ADD COLUMN retired BIT NOT NULL DEFAULT 0,
    ADD COLUMN board   BIT NOT NULL DEFAULT 0;
ALTER TABLE task_category_audit
    ADD COLUMN retired BIT NULL,
    ADD COLUMN board   BIT NULL;

ALTER TABLE task
    ADD COLUMN retired BIT NOT NULL DEFAULT 0;
ALTER TABLE task_audit
    ADD COLUMN retired BIT NULL;

-- Posters and logos were images of their own; Spexipedia has them now
UPDATE spex_details SET poster_id = NULL;
UPDATE spex_category SET logo_id = NULL;
DELETE FROM image WHERE id NOT IN (SELECT image_id FROM spexare WHERE image_id IS NOT NULL);
ALTER TABLE spex_details
    DROP FOREIGN KEY FK_SPEX_DETAILS_ON_POSTER,
    DROP INDEX UC_SPEX_DETAILS_POSTER,
    DROP COLUMN poster_id;
ALTER TABLE spex_details_audit DROP COLUMN poster_id;
ALTER TABLE spex_category
    DROP FOREIGN KEY FK_SPEX_CATEGORY_ON_LOGO,
    DROP INDEX UC_SPEX_CATEGORY_LOGO,
    DROP COLUMN logo_id;
ALTER TABLE spex_category_audit DROP COLUMN logo_id;

-- Characters, copied from Spexipedia like base data; actors point at them
CREATE TABLE IF NOT EXISTS spex_character
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    uuid             CHAR(36)              NOT NULL,
    spex_id          BIGINT                NOT NULL,
    name             VARCHAR(255)          NOT NULL,
    position         INT                   NOT NULL,
    retired          BIT                   NOT NULL DEFAULT 0,
    created_by       VARCHAR(255)          NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(255)          NULL,
    last_modified_at DATETIME              NULL,
    version          BIGINT                NULL,
    CONSTRAINT PK_SPEX_CHARACTER PRIMARY KEY (id),
    CONSTRAINT UC_SPEX_CHARACTER_UUID UNIQUE (uuid),
    CONSTRAINT FK_SPEX_CHARACTER_ON_SPEX FOREIGN KEY (spex_id) REFERENCES spex (id)
);

CREATE TABLE IF NOT EXISTS spex_character_audit
(
    id           BIGINT       NOT NULL,
    uuid         CHAR(36)     NULL,
    spex_id      BIGINT       NULL,
    name         VARCHAR(255) NULL,
    position     INT          NULL,
    retired      BIT          NULL,
    rev          BIGINT       NOT NULL REFERENCES revinfo (id),
    revend       BIGINT       DEFAULT NULL REFERENCES revinfo (id),
    revtype      TINYINT      DEFAULT NULL,
    revend_tstmp DATETIME(6)  DEFAULT NULL,
    KEY IX_rev (rev),
    KEY IX_revend (revend),
    CONSTRAINT PK_SPEX_CHARACTER_AUDIT PRIMARY KEY (id, rev)
);

ALTER TABLE actor
    ADD COLUMN character_id BIGINT NULL,
    ADD CONSTRAINT FK_ACTOR_ON_CHARACTER FOREIGN KEY (character_id) REFERENCES spex_character (id);
ALTER TABLE actor_audit
    ADD COLUMN character_id BIGINT NULL;

-- Where the sync has got to in each source's change feed
CREATE TABLE IF NOT EXISTS sync_source
(
    name           VARCHAR(50)   NOT NULL,
    cursor_value   VARCHAR(100)  NULL,
    last_pulled_at DATETIME(6)   NULL,
    last_success   DATETIME(6)   NULL,
    last_error     VARCHAR(1000) NULL,
    applied        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT PK_SYNC_SOURCE PRIMARY KEY (name)
);

INSERT INTO sync_source (name) VALUES ('spexipedia');

-- A user is linked to their spexare automatically by email unless an admin has removed a link by hand
ALTER TABLE user
    ADD COLUMN spexare_auto_link BIT NOT NULL DEFAULT 1;
ALTER TABLE user_audit
    ADD COLUMN spexare_auto_link BIT NULL;
