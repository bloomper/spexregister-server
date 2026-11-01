-- Every entity that leaves the system gets a stable external id; the BIGINT id stays internal.

ALTER TABLE news ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE news SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE news MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE news ADD CONSTRAINT UC_NEWS_UUID UNIQUE (uuid);
ALTER TABLE news_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE news_audit a JOIN news e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM news_audit WHERE uuid IS NULL) d;
UPDATE news_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_NEWS_AUDIT_ON_UUID ON news_audit (uuid);

ALTER TABLE spex ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE spex MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE spex ADD CONSTRAINT UC_SPEX_UUID UNIQUE (uuid);
ALTER TABLE spex_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex_audit a JOIN spex e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM spex_audit WHERE uuid IS NULL) d;
UPDATE spex_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_SPEX_AUDIT_ON_UUID ON spex_audit (uuid);

ALTER TABLE spex_details ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex_details SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE spex_details MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE spex_details ADD CONSTRAINT UC_SPEX_DETAILS_UUID UNIQUE (uuid);
ALTER TABLE spex_details_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex_details_audit a JOIN spex_details e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM spex_details_audit WHERE uuid IS NULL) d;
UPDATE spex_details_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_SPEX_DETAILS_AUDIT_ON_UUID ON spex_details_audit (uuid);

ALTER TABLE spex_category ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex_category SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE spex_category MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE spex_category ADD CONSTRAINT UC_SPEX_CATEGORY_UUID UNIQUE (uuid);
ALTER TABLE spex_category_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex_category_audit a JOIN spex_category e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM spex_category_audit WHERE uuid IS NULL) d;
UPDATE spex_category_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_SPEX_CATEGORY_AUDIT_ON_UUID ON spex_category_audit (uuid);

ALTER TABLE tag ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE tag SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE tag MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE tag ADD CONSTRAINT UC_TAG_UUID UNIQUE (uuid);
ALTER TABLE tag_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE tag_audit a JOIN tag e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM tag_audit WHERE uuid IS NULL) d;
UPDATE tag_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_TAG_AUDIT_ON_UUID ON tag_audit (uuid);

ALTER TABLE task ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE task SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE task MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE task ADD CONSTRAINT UC_TASK_UUID UNIQUE (uuid);
ALTER TABLE task_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE task_audit a JOIN task e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM task_audit WHERE uuid IS NULL) d;
UPDATE task_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_TASK_AUDIT_ON_UUID ON task_audit (uuid);

ALTER TABLE task_category ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE task_category SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE task_category MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE task_category ADD CONSTRAINT UC_TASK_CATEGORY_UUID UNIQUE (uuid);
ALTER TABLE task_category_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE task_category_audit a JOIN task_category e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM task_category_audit WHERE uuid IS NULL) d;
UPDATE task_category_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_TASK_CATEGORY_AUDIT_ON_UUID ON task_category_audit (uuid);

ALTER TABLE `user` ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE `user` SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE `user` MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE `user` ADD CONSTRAINT UC_USER_UUID UNIQUE (uuid);
ALTER TABLE user_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE user_audit a JOIN `user` e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM user_audit WHERE uuid IS NULL) d;
UPDATE user_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_USER_AUDIT_ON_UUID ON user_audit (uuid);

ALTER TABLE permission_grant ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE permission_grant SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE permission_grant MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE permission_grant ADD CONSTRAINT UC_PERMISSION_GRANT_UUID UNIQUE (uuid);
ALTER TABLE permission_grant_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE permission_grant_audit a JOIN permission_grant e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM permission_grant_audit WHERE uuid IS NULL) d;
UPDATE permission_grant_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_PERMISSION_GRANT_AUDIT_ON_UUID ON permission_grant_audit (uuid);

ALTER TABLE spexare ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spexare SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE spexare MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE spexare ADD CONSTRAINT UC_SPEXARE_UUID UNIQUE (uuid);
ALTER TABLE spexare_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spexare_audit a JOIN spexare e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM spexare_audit WHERE uuid IS NULL) d;
UPDATE spexare_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_SPEXARE_AUDIT_ON_UUID ON spexare_audit (uuid);

ALTER TABLE address ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE address SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE address MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE address ADD CONSTRAINT UC_ADDRESS_UUID UNIQUE (uuid);
ALTER TABLE address_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE address_audit a JOIN address e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM address_audit WHERE uuid IS NULL) d;
UPDATE address_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_ADDRESS_AUDIT_ON_UUID ON address_audit (uuid);

ALTER TABLE consent ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE consent SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE consent MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE consent ADD CONSTRAINT UC_CONSENT_UUID UNIQUE (uuid);
ALTER TABLE consent_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE consent_audit a JOIN consent e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM consent_audit WHERE uuid IS NULL) d;
UPDATE consent_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_CONSENT_AUDIT_ON_UUID ON consent_audit (uuid);

ALTER TABLE membership ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE membership SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE membership MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE membership ADD CONSTRAINT UC_MEMBERSHIP_UUID UNIQUE (uuid);
ALTER TABLE membership_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE membership_audit a JOIN membership e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM membership_audit WHERE uuid IS NULL) d;
UPDATE membership_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_MEMBERSHIP_AUDIT_ON_UUID ON membership_audit (uuid);

ALTER TABLE toggle ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE toggle SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE toggle MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE toggle ADD CONSTRAINT UC_TOGGLE_UUID UNIQUE (uuid);
ALTER TABLE toggle_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE toggle_audit a JOIN toggle e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM toggle_audit WHERE uuid IS NULL) d;
UPDATE toggle_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_TOGGLE_AUDIT_ON_UUID ON toggle_audit (uuid);

ALTER TABLE activity ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE activity SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE activity MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE activity ADD CONSTRAINT UC_ACTIVITY_UUID UNIQUE (uuid);
ALTER TABLE activity_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE activity_audit a JOIN activity e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM activity_audit WHERE uuid IS NULL) d;
UPDATE activity_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_ACTIVITY_AUDIT_ON_UUID ON activity_audit (uuid);

ALTER TABLE spex_activity ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex_activity SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE spex_activity MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE spex_activity ADD CONSTRAINT UC_SPEX_ACTIVITY_UUID UNIQUE (uuid);
ALTER TABLE spex_activity_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE spex_activity_audit a JOIN spex_activity e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM spex_activity_audit WHERE uuid IS NULL) d;
UPDATE spex_activity_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_SPEX_ACTIVITY_AUDIT_ON_UUID ON spex_activity_audit (uuid);

ALTER TABLE task_activity ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE task_activity SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE task_activity MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE task_activity ADD CONSTRAINT UC_TASK_ACTIVITY_UUID UNIQUE (uuid);
ALTER TABLE task_activity_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE task_activity_audit a JOIN task_activity e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM task_activity_audit WHERE uuid IS NULL) d;
UPDATE task_activity_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_TASK_ACTIVITY_AUDIT_ON_UUID ON task_activity_audit (uuid);

ALTER TABLE actor ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE actor SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE actor MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE actor ADD CONSTRAINT UC_ACTOR_UUID UNIQUE (uuid);
ALTER TABLE actor_audit ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE actor_audit a JOIN actor e ON a.id = e.id SET a.uuid = e.uuid;
CREATE TEMPORARY TABLE deleted_uuid AS SELECT d.id, UUID() AS uuid FROM (SELECT DISTINCT id FROM actor_audit WHERE uuid IS NULL) d;
UPDATE actor_audit a JOIN deleted_uuid g ON a.id = g.id SET a.uuid = g.uuid;
DROP TEMPORARY TABLE deleted_uuid;
CREATE INDEX IX_ACTOR_AUDIT_ON_UUID ON actor_audit (uuid);

ALTER TABLE saved_search ADD COLUMN uuid CHAR(36) NULL AFTER id;
UPDATE saved_search SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE saved_search MODIFY uuid CHAR(36) NOT NULL;
ALTER TABLE saved_search ADD CONSTRAINT UC_SAVED_SEARCH_UUID UNIQUE (uuid);
