package nu.fgv.register.server.acl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PermissionService {

    private final MutableAclService mutableAclService;

    public void grantPermission(final ObjectIdentity oid, final Permission permission, final Sid... recipients) {
        Arrays.asList(recipients).forEach(r -> grantPermission(oid, r, permission));
    }

    public void grantPermission(final ObjectIdentity oid, final Sid recipient, final Permission permission) {
        MutableAcl acl;

        try {
            acl = (MutableAcl) mutableAclService.readAclById(oid);
        } catch (final NotFoundException e) {
            acl = mutableAclService.createAcl(oid);
        }

        acl.insertAce(acl.getEntries().size(), permission, recipient, true);
        mutableAclService.updateAcl(acl);
    }

    public void revokePermission(final ObjectIdentity oid, final Sid recipient, final Permission permission) {
        try {
            final MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
            final List<AccessControlEntry> aclEntries = acl.getEntries();

            for (int i = aclEntries.size() - 1; i >= 0; i--) {
                AccessControlEntry ace = aclEntries.get(i);
                if (ace.getSid().equals(recipient) && ace.getPermission().equals(permission)) {
                    acl.deleteAce(i);
                }
            }
            if (acl.getEntries().isEmpty()) {
                mutableAclService.deleteAcl(oid, true);
            }
            mutableAclService.updateAcl(acl);
        } catch (final NotFoundException e) {
            // Ignore
        }
    }

    public void revokePermissions(final ObjectIdentity oid, final Sid recipient) {
        try {
            final MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
            final List<AccessControlEntry> aclEntries = acl.getEntries();

            for (int i = aclEntries.size() - 1; i >= 0; i--) {
                AccessControlEntry ace = aclEntries.get(i);
                if (ace.getSid().equals(recipient)) {
                    acl.deleteAce(i);
                }
            }
            if (acl.getEntries().isEmpty()) {
                mutableAclService.deleteAcl(oid, true);
            }
            mutableAclService.updateAcl(acl);
        } catch (final NotFoundException e) {
            // Ignore
        }
    }

    public void deleteAcl(final ObjectIdentity oid) {
        try {
            mutableAclService.deleteAcl(oid, true);
        } catch (final NotFoundException e) {
            // Ignore
        }
    }
}
