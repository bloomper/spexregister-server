/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.acl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PermissionService {

    private final MutableAclService mutableAclService;
    private final PermissionEvaluator permissionEvaluator;

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

        final boolean alreadyExists = acl.getEntries().stream()
                .anyMatch(entry -> entry.getSid().equals(recipient) && entry.getPermission().equals(permission));

        if (!alreadyExists) {
            acl.insertAce(acl.getEntries().size(), permission, recipient, true);
            mutableAclService.updateAcl(acl);
        }
    }

    public void revokePermission(final ObjectIdentity oid, final Permission permission, final Sid... recipients) {
        Arrays.asList(recipients).forEach(r -> revokePermission(oid, r, permission));
    }

    public void revokePermission(final ObjectIdentity oid, final Sid recipient, final Permission permission) {
        try {
            final MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
            final List<AccessControlEntry> aclEntries = acl.getEntries();

            for (int i = aclEntries.size() - 1; i >= 0; i--) {
                final AccessControlEntry ace = aclEntries.get(i);

                if (ace.getSid().equals(recipient) && ace.getPermission().equals(permission)) {
                    acl.deleteAce(i);
                }
            }
            if (acl.getEntries().isEmpty()) {
                mutableAclService.deleteAcl(oid, true);
            }
            mutableAclService.updateAcl(acl);
        } catch (final NotFoundException _) {
            // Ignore
        }
    }

    public void revokePermissions(final ObjectIdentity oid, final Sid recipient) {
        try {
            final MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
            final List<AccessControlEntry> aclEntries = acl.getEntries();

            for (int i = aclEntries.size() - 1; i >= 0; i--) {
                final AccessControlEntry ace = aclEntries.get(i);

                if (ace.getSid().equals(recipient)) {
                    acl.deleteAce(i);
                }
            }
            if (acl.getEntries().isEmpty()) {
                mutableAclService.deleteAcl(oid, true);
            }
            mutableAclService.updateAcl(acl);
        } catch (final NotFoundException _) {
            // Ignore
        }
    }

    public void deleteAcl(final ObjectIdentity oid) {
        try {
            mutableAclService.deleteAcl(oid, true);
        } catch (final NotFoundException _) {
            // Ignore
        }
    }

    public boolean hasPermission(final ObjectIdentity oid, final Sid recipient, final Permission permission) {
        try {
            final MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
            return acl.getEntries().stream()
                    .anyMatch(e -> e.getSid().equals(recipient) && e.getPermission().equals(permission));
        } catch (final NotFoundException _) {
            // Ignore
            return false;
        }
    }

    public <T> boolean hasReadPermission(final T object) {
        return hasPermission(object, BasePermission.READ) || hasAdministrationPermission(object);
    }

    public <T> T checkReadPermission(final T object) {
        if (hasReadPermission(object)) {
            return object;
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    public <T> boolean hasWritePermission(final T object) {
        return hasPermission(object, BasePermission.WRITE) || hasAdministrationPermission(object);
    }

    public <T> T checkWritePermission(final T object) {
        if (hasWritePermission(object)) {
            return object;
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    public <T> boolean hasDeletePermission(final T object) {
        return hasPermission(object, BasePermission.DELETE) || hasAdministrationPermission(object);
    }

    public <T> T checkDeletePermission(final T object) {
        if (hasDeletePermission(object)) {
            return object;
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    public <T> boolean hasAdministrationPermission(final T object) {
        return hasPermission(object, BasePermission.ADMINISTRATION);
    }

    public <T> T checkAdministrationPermission(final T object) {
        if (hasAdministrationPermission(object)) {
            return object;
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }

    public <T> boolean hasPermission(final T object, final Permission permission) {
        return permissionEvaluator.hasPermission(SecurityContextHolder.getContext().getAuthentication(), object, permission);
    }

}
