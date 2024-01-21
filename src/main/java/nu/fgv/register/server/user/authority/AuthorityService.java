package nu.fgv.register.server.user.authority;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;

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

    public List<AuthorityDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream().map(AUTHORITY_MAPPER::toDto)
                .toList();
    }

    public Optional<AuthorityDto> findById(final String id) {
        return repository
                .findById(id)
                .map(AUTHORITY_MAPPER::toDto);
    }

    @Cacheable("roleRepresentations")
    public RoleRepresentation getRoleRepresentationById(final String id) {
        final List<RoleRepresentation> roles = keycloakAdminClient.realm(keycloakRealm).clients().get(keycloakClientId).roles().list();

        return roles.stream()
                .filter(r -> r.getName().equals(id))
                .findFirst()
                .orElse(null); // Should never happen
    }

}
