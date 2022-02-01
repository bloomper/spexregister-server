CREATE TABLE IF NOT EXISTS acl_sid
(
    id        BIGINT(20)   NOT NULL AUTO_INCREMENT,
    principal TINYINT(1)   NOT NULL,
    sid       VARCHAR(100) NOT NULL,
    CONSTRAINT pk_acl_sid PRIMARY KEY (id)
);

ALTER TABLE acl_sid
    ADD CONSTRAINT uc_sid_principal UNIQUE (sid, principal);

CREATE TABLE IF NOT EXISTS acl_class
(
    id    BIGINT(20)   NOT NULL AUTO_INCREMENT,
    class varchar(255) NOT NULL,
    CONSTRAINT pk_acl_class PRIMARY KEY (id)
);

ALTER TABLE acl_class
    ADD CONSTRAINT uc_class UNIQUE (class);

CREATE TABLE IF NOT EXISTS acl_entry
(
    id                  BIGINT(20) NOT NULL AUTO_INCREMENT,
    acl_object_identity BIGINT(20) NOT NULL,
    ace_order           INT(11)    NOT NULL,
    sid                 BIGINT(20) NOT NULL,
    mask                INT(11)    NOT NULL,
    granting            TINYINT(1) NOT NULL,
    audit_success       TINYINT(1) NOT NULL,
    audit_failure       TINYINT(1) NOT NULL,
    CONSTRAINT pk_acl_entry PRIMARY KEY (id)
);

ALTER TABLE acl_entry
    ADD CONSTRAINT uc_acl_object_identity_ace_order UNIQUE (acl_object_identity, ace_order);

CREATE TABLE IF NOT EXISTS acl_object_identity
(
    id                 BIGINT(20) NOT NULL AUTO_INCREMENT,
    object_id_class    BIGINT(20) NOT NULL,
    object_id_identity BIGINT(20) NOT NULL,
    parent_object      BIGINT(20) DEFAULT NULL,
    owner_sid          BIGINT(20) DEFAULT NULL,
    entries_inheriting TINYINT(1) NOT NULL,
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
