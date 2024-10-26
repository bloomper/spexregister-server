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

package nu.fgv.register.server.user.authority;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AuthorityService {

    private final AuthorityRepository repository;
    private final Keycloak keycloakAdminClient;
    private final String keycloakClientId;
    @Value("${spexregister.keycloak.realm}")
    private String keycloakRealm;

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<AuthorityDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream().map(AUTHORITY_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<AuthorityDto> findById(final String id) {
        return repository
                .findById(id)
                .map(AUTHORITY_MAPPER::toDto);
    }

    @Cacheable("roleRepresentations")
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public RoleRepresentation getRoleRepresentationById(final String id) {
        final List<RoleRepresentation> roles = keycloakAdminClient.realm(keycloakRealm).clients().get(keycloakClientId).roles().list();

        return roles.stream()
                .filter(r -> r.getName().equals(id))
                .findFirst()
                .orElse(null); // Should never happen
    }

}
