-- The SPEX permission grant scope goes. Base data is read only here (Spexipedia keeps it, spexregister-spec,
-- docs/base-data-and-memberships.md, section 3), so a grant on a spex could at most let someone read it, which every
-- role does anyway. SPEX_PARTICIPANTS grants stay, and still name a spex.

-- Their history goes with them: Envers could no longer read the scope back, so the revisions could not be shown.
DELETE FROM permission_grant_audit
WHERE id IN (SELECT id FROM (SELECT DISTINCT id FROM permission_grant_audit WHERE scope_type = 'SPEX') AS spex_grants);
DELETE FROM revchanges
WHERE entityname = 'nu.fgv.register.server.permission.PermissionGrant'
  AND rev NOT IN (SELECT DISTINCT rev FROM permission_grant_audit);

DELETE FROM permission_grant WHERE scope_type = 'SPEX';

ALTER TABLE permission_grant DROP CHECK CK_PERMISSION_GRANT_SCOPE;
ALTER TABLE permission_grant
    ADD CONSTRAINT CK_PERMISSION_GRANT_SCOPE CHECK (
        (scope_type = 'SPEXARE' AND spexare_id IS NOT NULL AND spex_id IS NULL)
            OR (scope_type = 'SPEX_PARTICIPANTS' AND spex_id IS NOT NULL AND spexare_id IS NULL));
