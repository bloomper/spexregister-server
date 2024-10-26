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
                final AccessControlEntry ace = aclEntries.get(i);

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
                final AccessControlEntry ace = aclEntries.get(i);

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

    public boolean hasPermission(final ObjectIdentity oid, final Sid recipient, final Permission permission) {
        try {
            final MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
            return acl.getEntries().stream()
                    .anyMatch(e -> e.getSid().equals(recipient) && e.getPermission().equals(permission));
        } catch (final NotFoundException e) {
            // Ignore
            return false;
        }
    }
}
