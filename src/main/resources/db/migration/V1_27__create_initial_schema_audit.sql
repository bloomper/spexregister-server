CREATE TABLE IF NOT EXISTS revinfo
(
    id                     BIGINT                NOT NULL AUTO_INCREMENT,
    modified_at            BIGINT                NOT NULL,
    modified_by            VARCHAR(255)          DEFAULT NULL,
    CONSTRAINT PK_REV_INFO PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS revchanges
(
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    entityname             VARCHAR(255)          DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS type_audit
(
    id                     VARCHAR(255)          NOT NULL,
    type                   VARCHAR(255)          NOT NULL,
    labels                 JSON                  NOT NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_TYPE PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS state_audit
(
    id                     VARCHAR(255)          NOT NULL,
    labels                 JSON                  NOT NULL,
    initial                BIT                   DEFAULT 0,
    enabled                BIT                   DEFAULT 0,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_STATE PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS user_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    external_id            VARCHAR(255)          NOT NULL,
    state_id               VARCHAR(255)          DEFAULT 'PENDING',
    spexare_id             BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_USER PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS tag_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    name                   VARCHAR(255)          NOT NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_TAG PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS news_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    visible_from           date                  NULL,
    visible_to             date                  NULL,
    subject                VARCHAR(255)          NOT NULL,
    text                   LONGTEXT              NOT NULL,
    published              BIT                   NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_NEWS PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS spex_category_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    name                   VARCHAR(255)          NOT NULL,
    first_year             VARCHAR(4)            NOT NULL,
    logo                   MEDIUMBLOB            NULL,
    logo_content_type      VARCHAR(255)          NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_SPEX_CATEGORY PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS spex_details_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    title                  VARCHAR(255)          NOT NULL,
    poster                 MEDIUMBLOB            NULL,
    poster_content_type    VARCHAR(255)          NULL,
    category_id            BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_SPEX_DETAILS PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS spex_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    year                   VARCHAR(4)            NOT NULL,
    parent_id              BIGINT                NULL,
    details_id             BIGINT                NOT NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_SPEX PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS task_category_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    name                   VARCHAR(255)          NOT NULL,
    actor_present          BIT                   NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_TASK_CATEGORY PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS task_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    name                   VARCHAR(255)          NOT NULL,
    category_id            BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_TASK PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS spexare_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    first_name             VARCHAR(255)          NOT NULL,
    last_name              VARCHAR(255)          NOT NULL,
    nick_name              VARCHAR(255)          NULL,
    social_security_number VARCHAR(255)          NULL,
    deceased               BIT                   DEFAULT 0,
    published              BIT                   DEFAULT 1,
    graduation             VARCHAR(255)          NULL,
    comment                LONGTEXT              NULL,
    image                  MEDIUMBLOB            NULL,
    image_content_type     VARCHAR(255)          NULL,
    partner_id             BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_SPEXARE PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS toggle_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    value                  BIT                   NOT NULL,
    type_id                VARCHAR(255)          NOT NULL,
    spexare_id             BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_TOGGLE PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS tagging_audit
(
    tag_id                 BIGINT NOT NULL,
    spexare_id             BIGINT NOT NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_TAGGING PRIMARY KEY (tag_id, spexare_id, rev)
);

CREATE TABLE IF NOT EXISTS membership_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    year                   VARCHAR(4)            NOT NULL,
    type_id                VARCHAR(255)          NOT NULL,
    spexare_id             BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_MEMBERSHIP PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS consent_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    value                  BIT                   NOT NULL,
    type_id                VARCHAR(255)          NOT NULL,
    spexare_id             BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_CONSENT PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS address_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    street_address         VARCHAR(255)          NULL,
    postal_code            VARCHAR(255)          NULL,
    city                   VARCHAR(255)          NULL,
    country                VARCHAR(2)            NULL,
    phone                  VARCHAR(255)          NULL,
    phone_mobile           VARCHAR(255)          NULL,
    email_address          VARCHAR(255)          NULL,
    type_id                VARCHAR(255)          NOT NULL,
    spexare_id             BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_ADDRESS PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS activity_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    spexare_id             BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_ACTIVITY PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS spex_activity_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    activity_id            BIGINT                NOT NULL,
    spex_id                BIGINT                NOT NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_SPEX_ACTIVITY PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS task_activity_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    activity_id            BIGINT                NOT NULL,
    task_id                BIGINT                NOT NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_TASK_ACTIVITY PRIMARY KEY (id, rev)
);

CREATE TABLE IF NOT EXISTS actor_audit
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    role                   VARCHAR(255)          NULL,
    vocal_id               VARCHAR(255)          NOT NULL,
    task_activity_id       BIGINT                NULL,
    rev                    BIGINT                NOT NULL REFERENCES revinfo (id),
    revend                 BIGINT                DEFAULT NULL REFERENCES revinfo (id),
    revtype                TINYINT               DEFAULT NULL,
    revend_tstmp           DATETIME(6)           DEFAULT NULL,

    KEY                    IX_rev                (rev),
    KEY                    IX_revend             (revend),

    CONSTRAINT PK_ACTOR PRIMARY KEY (id, rev)
);
