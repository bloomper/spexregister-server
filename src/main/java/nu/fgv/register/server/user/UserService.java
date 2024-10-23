package nu.fgv.register.server.user;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.authority.AuthorityRepository;
import nu.fgv.register.server.user.authority.AuthorityService;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.ResourceAlreadyExistsException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;
import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;
import static nu.fgv.register.server.user.state.StateMapper.STATE_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.passay.AllowedCharacterRule.ERROR_CODE;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final AuthorityRepository authorityRepository;
    private final StateRepository stateRepository;
    private final SpexareRepository spexareRepository;
    private final PermissionService permissionService;
    private final AuthorityService authorityService;
    private final Keycloak keycloakAdminClient;
    private final String keycloakClientId;
    @Value("${spexregister.keycloak.realm}")
    private String keycloakRealm;

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Page<UserDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<User>builder().build(FilterParser.parse(filter), UserSpecification::new), pageable, BasePermission.READ)
                        .map(this::joinModelWithRepresentation) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(this::joinModelWithRepresentation);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<UserDto> findById(final Long id) {
        return repository
                .findById0(id)
                .flatMap(model ->
                        findResourceByExternalId(model.getExternalId())
                                .map(resource -> USER_MAPPER.toDto(model, resource.toRepresentation(), null))
                );
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<UserDto> create(final UserCreateDto dto) {
        if (!doesUserWithEmailExist(dto.getEmail())) {
            final String temporaryPassword = generateTemporaryPassword();

            try (final Response response = keycloakAdminClient
                    .realm(keycloakRealm)
                    .users()
                    .create(USER_MAPPER.toRepresentation(dto, temporaryPassword))
            ) {
                if (response.getStatus() == HttpStatus.CREATED.value()) {
                    final String locationPath = response.getLocation().getPath();
                    final String externalId = locationPath.substring(locationPath.lastIndexOf('/') + 1);

                    return findResourceByExternalId(externalId)
                            .map(resource -> {
                                final User model = repository.save(USER_MAPPER.toModel(externalId));
                                final ObjectIdentity oid = toObjectIdentity(User.class, model.getId());

                                permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);

                                return USER_MAPPER.toDto(model, resource.toRepresentation(), temporaryPassword);
                            });
                } else {
                    return Optional.empty();
                }
            }
        } else {
            throw new ResourceAlreadyExistsException(String.format("User with email %s already exists", dto.getEmail()));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<UserDto> update(final UserUpdateDto dto) {
        return partialUpdate(dto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<UserDto> partialUpdate(final UserUpdateDto dto) {
        if (!doesUserWithEmailExist(dto.getEmail())) {
            return repository
                    .findById0(dto.getId())
                    .map(user -> {
                        USER_MAPPER.toPartialModel(dto, user);
                        return user;
                    })
                    .map(repository::save)
                    .map(model -> {
                        findResourceByExternalId(model.getExternalId())
                                .ifPresent(resource -> {
                                    final UserRepresentation representation = resource.toRepresentation();

                                    representation.setEmail(dto.getEmail());
                                    resource.update(representation);
                                });
                        return findResourceByExternalId(model.getExternalId())
                                .map(resource -> USER_MAPPER.toDto(model, resource.toRepresentation(), null))
                                .orElse(null);
                    });
        } else {
            throw new ResourceAlreadyExistsException(String.format("User with email %s already exists", dto.getEmail()));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public void deleteById(final Long id) {
        repository.findById0(id)
                .flatMap(model -> findResourceByExternalId(model.getExternalId()))
                .ifPresent(UserResource::remove);
        repository.deleteById(id);
        permissionService.deleteAcl(toObjectIdentity(User.class, id));
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Set<AuthorityDto> getAuthoritiesByUser(final Long userId) {
        if (doesUserExist(userId)) {
            return repository.findById0(userId)
                    .flatMap(model -> findResourceByExternalId(model.getExternalId()))
                    .map(resource -> {
                        final List<RoleRepresentation> roleRepresentations = resource
                                .roles()
                                .clientLevel(keycloakClientId)
                                .listAll();

                        return authorityRepository.findAll().stream()
                                .filter(authority -> roleRepresentations.stream().anyMatch(r -> r.getName().equals(authority.getId())))
                                .collect(Collectors.toSet());
                    })
                    .map(AUTHORITY_MAPPER::toDtos)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("User %s does not exist in Keycloak", userId)));
        } else {
            throw new ResourceNotFoundException(String.format("User %s does not exist", userId));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean addAuthorities(final Long userId, final List<String> ids) {
        if (doUserAndAuthoritiesExist(userId, ids)) {
            return repository.findById0(userId)
                    .flatMap(model -> findResourceByExternalId(model.getExternalId()))
                    .map(resource -> {
                        final List<RoleRepresentation> roleRepresentations = resource
                                .roles()
                                .clientLevel(keycloakClientId)
                                .listAll();

                        if (roleRepresentations.isEmpty() || ids.stream().noneMatch(i -> roleRepresentations.stream().noneMatch(r -> i.equals(r.getName())))) {
                            final List<RoleRepresentation> rolesToAdd = ids.stream()
                                    .map(authorityService::getRoleRepresentationById)
                                    .toList();

                            if (!rolesToAdd.isEmpty()) {
                                resource
                                        .roles()
                                        .clientLevel(keycloakClientId)
                                        .add(rolesToAdd);

                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    })
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("User %s does not exist in Keycloak", userId)));
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or at least one authority in %s do not exist", userId, String.join(",", ids)));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean addAuthority(final Long userId, final String id) {
        return addAuthorities(userId, List.of(id));
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean removeAuthorities(final Long userId, final List<String> ids) {
        if (doUserAndAuthoritiesExist(userId, ids)) {
            return repository.findById0(userId)
                    .flatMap(model -> findResourceByExternalId(model.getExternalId()))
                    .map(resource -> {
                        final List<RoleRepresentation> roleRepresentations = resource
                                .roles()
                                .clientLevel(keycloakClientId)
                                .listAll();

                        if (ids.stream().allMatch(i -> roleRepresentations.stream().anyMatch(r -> i.equals(r.getName())))) {
                            final List<RoleRepresentation> rolesToRemove = roleRepresentations.stream()
                                    .filter(r -> ids.contains(r.getName()))
                                    .toList();

                            if (!rolesToRemove.isEmpty()) {
                                resource
                                        .roles()
                                        .clientLevel(keycloakClientId)
                                        .remove(rolesToRemove);

                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    })
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("User %s does not exist in Keycloak", userId)));
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or at least one authority in %s do not exist", userId, String.join(",", ids)));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean removeAuthority(final Long userId, final String id) {
        return removeAuthorities(userId, List.of(id));
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public StateDto getStateByUser(final Long id) {
        return repository
                .findById0(id)
                .map(User::getState)
                .map(STATE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User %s does not exist", id)));
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean setState(final Long userId, final String id) {
        if (doUserAndStateExist(userId, id)) {
            return repository
                    .findById0(userId)
                    .map(user -> stateRepository
                            .findById(id)
                            .map(state -> {
                                user.setState(state);
                                repository.save(user);
                                return true;
                            })
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or state %s does not exist", userId, id));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<SpexareDto> findSpexareByUser(final Long userId) {
        if (doesUserExist(userId)) {
            return repository
                    .findById0(userId)
                    .map(User::getSpexare)
                    .map(SPEXARE_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("User %s does not exist", userId));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean addSpexare(final Long userId, final Long id) {
        if (doUserAndSpexareExist(userId, id)) {
            return repository
                    .findById0(userId)
                    .map(user -> spexareRepository
                            .findById(id)
                            .map(spexare -> {
                                user.setSpexare(spexare);
                                repository.save(user);
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or spexare %s do not exist", userId, id));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean removeSpexare(final Long userId) {
        if (doesUserExist(userId)) {
            return repository
                    .findById0(userId)
                    .filter(user -> user.getSpexare() != null)
                    .map(user -> {
                        user.setSpexare(null);
                        repository.save(user);
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s does not exist", userId));
        }
    }

    @Scheduled(cron = "${spexregister.jobs.sync-users.cron-expression}")
    public void scheduledSync() {
        final Set<String> alreadyAdded = new HashSet<>();

        authorityRepository.findAll()
                .forEach(a ->
                        keycloakAdminClient
                                .realm(keycloakRealm)
                                .clients()
                                .get(keycloakClientId)
                                .roles()
                                .get(a.getId())
                                .getUserMembers()
                                .stream()
                                .filter(r -> !alreadyAdded.contains(r.getId()))
                                .forEach(representation -> {
                                    if (!repository.existsByExternalId(representation.getId())) {
                                        final User model = repository.save(USER_MAPPER.toModel(representation.getId()));
                                        final ObjectIdentity oid = toObjectIdentity(User.class, model.getId());

                                        permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                                        alreadyAdded.add(representation.getId());
                                        log.info("Synced user {} from Keycloak", representation.getId());
                                    }
                                })
                );
    }

    private Optional<UserResource> findResourceByExternalId(final String externalId) {
        try {
            final UserResource userResource = keycloakAdminClient
                    .realm(keycloakRealm)
                    .users()
                    .get(externalId);

            return Optional.ofNullable(userResource);
        } catch (final Exception e) {
            log.error("Error while retrieving user info from Keycloak for external id {}", externalId, e);
        }
        return Optional.empty();
    }

    private UserDto joinModelWithRepresentation(final User model) {
        return USER_MAPPER.toDto(
                model,
                findResourceByExternalId(model.getExternalId())
                        .map(UserResource::toRepresentation)
                        .orElseGet(UserRepresentation::new),
                null
        );
    }

    private boolean doesUserExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doUserAndAuthoritiesExist(final Long userId, final List<String> authorityIds) {
        return doesUserExist(userId) && authorityIds.stream().allMatch(authorityRepository::existsById);
    }

    private boolean doUserAndStateExist(final Long userId, final String stateId) {
        return doesUserExist(userId) && stateRepository.existsById(stateId);
    }

    private boolean doUserAndSpexareExist(final Long userId, final Long spexareId) {
        return doesUserExist(userId) && spexareRepository.existsById(spexareId);
    }

    private boolean doesUserWithEmailExist(final String email) {
        final List<UserRepresentation> users = keycloakAdminClient.realm(keycloakRealm).users().searchByEmail(email, true);

        return users != null && !users.isEmpty();
    }

    private String generateTemporaryPassword() {
        final PasswordGenerator passwordGenerator = new PasswordGenerator();

        final CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase);
        lowerCaseRule.setNumberOfCharacters(2);

        final CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase);
        upperCaseRule.setNumberOfCharacters(2);

        final CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit);
        digitRule.setNumberOfCharacters(2);

        final CharacterRule specialCharacterRule = new CharacterRule(new CharacterData() {
            public String getErrorCode() {
                return ERROR_CODE;
            }

            public String getCharacters() {
                return "!@#$%^&*()_+";
            }
        });
        specialCharacterRule.setNumberOfCharacters(2);

        return passwordGenerator.generatePassword(15, List.of(specialCharacterRule, lowerCaseRule, upperCaseRule, digitRule));
    }
}
