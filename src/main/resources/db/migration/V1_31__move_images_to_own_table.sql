CREATE TABLE IF NOT EXISTS image
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    data          MEDIUMBLOB            NOT NULL,
    content_type  VARCHAR(255)          NOT NULL,
    migrated_from VARCHAR(64)           NULL,
    CONSTRAINT PK_IMAGE PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS image_audit
(
    id           BIGINT       NOT NULL,
    data         MEDIUMBLOB   NULL,
    content_type VARCHAR(255) NULL,
    rev          BIGINT       NOT NULL REFERENCES revinfo (id),
    revend       BIGINT       DEFAULT NULL REFERENCES revinfo (id),
    revtype      TINYINT      DEFAULT NULL,
    revend_tstmp DATETIME(6)  DEFAULT NULL,

    KEY          IX_rev (rev),
    KEY          IX_revend (revend),

    CONSTRAINT PK_IMAGE_AUDIT PRIMARY KEY (id, rev)
);

-- Current images move over; their history in the owners' audit tables is not carried along.
INSERT INTO image (data, content_type, migrated_from)
SELECT image, COALESCE(image_content_type, 'application/octet-stream'), CONCAT('spexare:', id)
FROM spexare
WHERE image IS NOT NULL;

INSERT INTO image (data, content_type, migrated_from)
SELECT poster, COALESCE(poster_content_type, 'application/octet-stream'), CONCAT('spex_details:', id)
FROM spex_details
WHERE poster IS NOT NULL;

INSERT INTO image (data, content_type, migrated_from)
SELECT logo, COALESCE(logo_content_type, 'application/octet-stream'), CONCAT('spex_category:', id)
FROM spex_category
WHERE logo IS NOT NULL;

ALTER TABLE spexare
    ADD image_id BIGINT NULL;
ALTER TABLE spex_details
    ADD poster_id BIGINT NULL;
ALTER TABLE spex_category
    ADD logo_id BIGINT NULL;

UPDATE spexare s JOIN image i ON i.migrated_from = CONCAT('spexare:', s.id)
SET s.image_id = i.id;
UPDATE spex_details d JOIN image i ON i.migrated_from = CONCAT('spex_details:', d.id)
SET d.poster_id = i.id;
UPDATE spex_category c JOIN image i ON i.migrated_from = CONCAT('spex_category:', c.id)
SET c.logo_id = i.id;

ALTER TABLE image
    DROP COLUMN migrated_from;

ALTER TABLE spexare
    DROP COLUMN image,
    DROP COLUMN image_content_type,
    ADD CONSTRAINT UC_SPEXARE_IMAGE UNIQUE (image_id),
    ADD CONSTRAINT FK_SPEXARE_ON_IMAGE FOREIGN KEY (image_id) REFERENCES image (id);
ALTER TABLE spex_details
    DROP COLUMN poster,
    DROP COLUMN poster_content_type,
    ADD CONSTRAINT UC_SPEX_DETAILS_POSTER UNIQUE (poster_id),
    ADD CONSTRAINT FK_SPEX_DETAILS_ON_POSTER FOREIGN KEY (poster_id) REFERENCES image (id);
ALTER TABLE spex_category
    DROP COLUMN logo,
    DROP COLUMN logo_content_type,
    ADD CONSTRAINT UC_SPEX_CATEGORY_LOGO UNIQUE (logo_id),
    ADD CONSTRAINT FK_SPEX_CATEGORY_ON_LOGO FOREIGN KEY (logo_id) REFERENCES image (id);

ALTER TABLE spexare_audit
    DROP COLUMN image,
    DROP COLUMN image_content_type,
    ADD image_id BIGINT NULL;
ALTER TABLE spex_details_audit
    DROP COLUMN poster,
    DROP COLUMN poster_content_type,
    ADD poster_id BIGINT NULL;
ALTER TABLE spex_category_audit
    DROP COLUMN logo,
    DROP COLUMN logo_content_type,
    ADD logo_id BIGINT NULL;
