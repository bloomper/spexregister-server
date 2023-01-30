CREATE TABLE IF NOT EXISTS acl_sid
(
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    principal TINYINT      NOT NULL,
    sid       VARCHAR(100) NOT NULL,
    CONSTRAINT pk_acl_sid PRIMARY KEY (id)
);

ALTER TABLE acl_sid
    ADD CONSTRAINT uc_sid_principal UNIQUE (sid, principal);

CREATE TABLE IF NOT EXISTS acl_class
(
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    class varchar(255) NOT NULL,
    CONSTRAINT pk_acl_class PRIMARY KEY (id)
);

ALTER TABLE acl_class
    ADD CONSTRAINT uc_class UNIQUE (class);

CREATE TABLE IF NOT EXISTS acl_entry
(
    id                  BIGINT  NOT NULL AUTO_INCREMENT,
    acl_object_identity BIGINT  NOT NULL,
    ace_order           INT     NOT NULL,
    sid                 BIGINT  NOT NULL,
    mask                INT     NOT NULL,
    granting            TINYINT NOT NULL,
    audit_success       TINYINT NOT NULL,
    audit_failure       TINYINT NOT NULL,
    CONSTRAINT pk_acl_entry PRIMARY KEY (id)
);

ALTER TABLE acl_entry
    ADD CONSTRAINT uc_acl_object_identity_ace_order UNIQUE (acl_object_identity, ace_order);

CREATE TABLE IF NOT EXISTS acl_object_identity
(
    id                 BIGINT  NOT NULL AUTO_INCREMENT,
    object_id_class    BIGINT  NOT NULL,
    object_id_identity BIGINT  NOT NULL,
    parent_object      BIGINT DEFAULT NULL,
    owner_sid          BIGINT DEFAULT NULL,
    entries_inheriting TINYINT NOT NULL,
    CONSTRAINT pk_acl_object_identity PRIMARY KEY (id)
);

ALTER TABLE acl_object_identity
    ADD CONSTRAINT uc_object_id_class_object_id_identity UNIQUE (object_id_class, object_id_identity);

ALTER TABLE acl_entry
    ADD CONSTRAINT FK_ACL_OBJECT_IDENTITY_ON_ACL_ENTRY FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity (id);

ALTER TABLE acl_entry
    ADD CONSTRAINT FK_ACL_SID_ON_ACL_ENTRY FOREIGN KEY (sid) REFERENCES acl_sid (id);

ALTER TABLE acl_object_identity
    ADD CONSTRAINT FK_ACL_OBJECT_IDENTITY_ON_ACL_OBJECT_IDENTITY FOREIGN KEY (parent_object) REFERENCES acl_object_identity (id);

ALTER TABLE acl_object_identity
    ADD CONSTRAINT FK_ACL_CLASS_ON_ACL_OBJECT_IDENTITY FOREIGN KEY (object_id_class) REFERENCES acl_class (id);

ALTER TABLE acl_object_identity
    ADD CONSTRAINT FK_ACL_SID_ON_ACL_OBJECT_IDENTITY FOREIGN KEY (owner_sid) REFERENCES acl_sid (id);
