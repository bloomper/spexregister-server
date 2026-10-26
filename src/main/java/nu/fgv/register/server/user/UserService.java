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

package nu.fgv.register.server.user;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.permission.PermissionGrantService;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.authority.Authority;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.authority.AuthorityRepository;
import nu.fgv.register.server.user.authority.AuthorityService;
import nu.fgv.register.server.user.state.State;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.error.ExternalResourceNotFoundException;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceAlreadyExistsException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.passay.data.CharacterData;
import org.passay.data.EnglishCharacterData;
import org.passay.generate.PasswordGenerator;
import org.passay.rule.CharacterRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import nu.fgv.register.server.security.policy.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;
import static nu.fgv.register.server.user.UserSpecification.NO_FILTER;
import static nu.fgv.register.server.user.UserSpecification.hasExternalIds;
import static nu.fgv.register.server.user.UserSpecification.hasIds;
import static nu.fgv.register.server.user.UserSpecification.hasSpexareIds;
import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;
import static nu.fgv.register.server.user.state.StateMapper.STATE_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;
import static org.passay.rule.AllowedCharacterRule.ERROR_CODE;
import static org.springframework.util.StringUtils.hasText;

/**
 * Users, kept in step with Keycloak. Creating, changing and removing a user also changes the Keycloak account, and
 * roles are Keycloak client roles. A scheduled job adds users who have been given a role in Keycloak and removes those
 * deleted there.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private static final int SYNC_PAGE_SIZE = 100;
    private static final int KEYCLOAK_SEARCH_FACTOR = 5;

    private final UserRepository repository;
    private final AuthorityRepository authorityRepository;
    private final StateRepository stateRepository;
    private final SpexareRepository spexareRepository;
    private final PermissionService permissionService;
    private final PermissionGrantService permissionGrantService;
    private final AuthorityService authorityService;
    private final Keycloak keycloakAdminClient;
    private final String keycloakClientId;
    private final PlatformTransactionManager transactionManager;
    @Value("${spexregister.keycloak.realm}")
    private String keycloakRealm;
    @Value("${spexregister.keycloak.pending-approval-group}")
    private String pendingApprovalGroup;

    /**
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresAdmin
    public CountedWindow<UserDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<User>builder().build(FilterParser.parse(filter), UserSpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(this::joinModelWithRepresentation), total);
                        });
    }

    /**
     * @param filter   a filter expression, or blank for all
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdmin
    public Page<UserDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<User>builder().build(FilterParser.parse(filter), UserSpecification::new), pageable, Permission.READ)
                        .map(this::joinModelWithRepresentation) :
                repository
                        .findAll(pageable, Permission.READ)
                        .map(this::joinModelWithRepresentation);
    }

    /**
     * Finds users to pick from by the email, username or name of their Keycloak account, or by the name of their
     * spexare.
     *
     * @param query the search text, blank for the first users
     * @param limit the most users to return
     * @return the users found, those matching in Keycloak first
     */
    @RequiresAdmin
    public List<UserDto> search(final String query, final int limit) {
        if (!hasText(query)) {
            return repository
                    .findAll(PageRequest.of(0, limit, Sort.by(User_.ID)), Permission.READ)
                    .map(this::joinModelWithRepresentation)
                    .getContent();
        }

        final List<String> externalIds = keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .search(query.trim(), 0, limit * KEYCLOAK_SEARCH_FACTOR, true)
                .stream()
                .map(UserRepresentation::getId)
                .toList();
        final List<Long> spexareIds = spexareRepository
                .search(query.trim(), List.of(), 0, limit, Sort.unsorted())
                .hits()
                .stream()
                .map(Spexare::getId)
                .toList();
        final Map<Long, User> found = new LinkedHashMap<>();

        if (!externalIds.isEmpty()) {
            repository.findAll(hasExternalIds(externalIds), Permission.READ).stream()
                    .sorted(Comparator.comparingInt(user -> externalIds.indexOf(user.getExternalId())))
                    .forEach(user -> found.putIfAbsent(user.getId(), user));
        }
        if (!spexareIds.isEmpty()) {
            repository.findAll(hasSpexareIds(spexareIds), Permission.READ).stream()
                    .sorted(Comparator.comparingInt(user -> spexareIds.indexOf(user.getSpexare().getId())))
                    .forEach(user -> found.putIfAbsent(user.getId(), user));
        }

        return found.values().stream()
                .limit(limit)
                .map(this::joinModelWithRepresentation)
                .toList();
    }

    /**
     * @param id the user id
     * @return the user
     */
    @RequiresAdmin
    public UserDto findById(final Long id) {
        return repository
                .findById0(id)
                .flatMap(model ->
                        findResourceByExternalId(model.getExternalId())
                                .map(resource -> USER_MAPPER.toDto(model, resource.toRepresentation(), null))
                )
                .orElseThrow(() -> new ResourceNotFoundException(User.class, id));
    }

    /**
     * @param externalId the Keycloak subject
     * @return the user, or null when no user has the subject or its Keycloak account is gone
     */
    @RequiresAdminOrEditorOrUser
    @Nullable
    public UserDto findByExternalId(final String externalId) {
        return repository
                .findByExternalId(externalId)
                .flatMap(model ->
                        findResourceByExternalId(model.getExternalId())
                                .map(resource -> USER_MAPPER.toDto(model, resource.toRepresentation(), null))
                )
                .orElse(null);
    }

    /**
     * @param user the user
     * @return the user's Keycloak account and Spexregister roles
     */
    @RequiresAdmin
    @Nullable
    public UserKeycloakData getKeycloakDataByUser(final User user) {
        return findResourceByExternalId(user.getExternalId())
                .map(resource -> {
                    final UserRepresentation representation = resource.toRepresentation();
                    final List<RoleRepresentation> roleRepresentations = resource
                            .roles()
                            .clientLevel(keycloakClientId)
                            .listAll();

                    return new UserKeycloakData(representation, extractRelevantAuthorities(roleRepresentations));
                })
                .orElse(null);
    }

    /**
     * @param id the user id
     * @return whether the user exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdmin
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    /**
     * @param ids    the ids of the users, or empty to use the filter
     * @param filter a filter expression, or blank for all
     * @param sort   the order
     * @return the users the caller may read, streamed when iterated, so it must be consumed inside a transaction
     */
    @RequiresAdmin
    public Iterable<User> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<User> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<User>builder()
                        .build(FilterParser.parse(filter), UserSpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * Creates the Keycloak account with a temporary password, enabled as the initial state says, and the user in that
     * state.
     *
     * @param dto the email
     * @return the created user, with the temporary password, which is not kept
     * @throws nu.fgv.register.server.util.error.ResourceAlreadyExistsException when the email is taken
     * @throws nu.fgv.register.server.util.error.InternalErrorException when Keycloak refuses the account
     */
    @RequiresAdmin
    public UserDto create(final UserCreateDto dto) {
        if (!doesUserWithEmailExist(dto.email(), null)) {
            final String temporaryPassword = generateTemporaryPassword();
            final State initialState = getUserInitialState();

            try (final Response response = keycloakAdminClient
                    .realm(keycloakRealm)
                    .users()
                    .create(USER_MAPPER.toRepresentation(dto, temporaryPassword, initialState.getEnabled()))
            ) {
                if (response.getStatus() == HttpStatus.CREATED.value()) {
                    final String locationPath = response.getLocation().getPath();
                    final String externalId = locationPath.substring(locationPath.lastIndexOf('/') + 1);

                    return findResourceByExternalId(externalId)
                            .map(resource -> {
                                final User model = repository.save(USER_MAPPER.toModel(externalId, initialState));

                                return USER_MAPPER.toDto(model, resource.toRepresentation(), temporaryPassword);
                            })
                            .orElseThrow(() -> new InternalErrorException("Could not retrieve newly created user in Keycloak"));
                } else {
                    throw new InternalErrorException("Could not create user in Keycloak");
                }
            }
        } else {
            throw new ResourceAlreadyExistsException(User.class, dto.email());
        }
    }

    /**
     * @param dto the new values
     * @return the updated user
     */
    @RequiresAdmin
    public UserDto update(final UserUpdateDto dto) {
        return partialUpdate(dto);
    }

    /**
     * Changes the email, in Keycloak too.
     *
     * @param dto the user id and new values
     * @return the updated user
     * @throws nu.fgv.register.server.util.error.ResourceAlreadyExistsException when the email is taken by another
     *     account
     */
    @RequiresAdmin
    public UserDto partialUpdate(final UserUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(user -> {
                    if (!doesUserWithEmailExist(dto.email(), user.getExternalId())) {
                        USER_MAPPER.toPartialModel(dto, user);
                        return user;
                    } else {
                        throw new ResourceAlreadyExistsException(User.class, dto.email());
                    }
                })
                .map(repository::save)
                .map(model -> {
                    findResourceByExternalId(model.getExternalId())
                            .ifPresent(resource -> {
                                final UserRepresentation representation = resource.toRepresentation();

                                representation.setEmail(dto.email());
                                resource.update(representation);
                            });
                    return findResourceByExternalId(model.getExternalId())
                            .map(resource -> USER_MAPPER.toDto(model, resource.toRepresentation(), null))
                            .orElseThrow(() -> new InternalErrorException("Could not update user"));
                })
                .orElseThrow(() -> new ResourceNotFoundException(User.class, dto.id()));

    }

    /**
     * Removes the Keycloak account, then the user and the permission grants to it. The user's spexare stays.
     *
     * @param id the user id
     */
    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesUserExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .flatMap(model -> findResourceByExternalId(model.getExternalId()))
                    .flatMap(resource -> {
                        resource.remove();
                        return repository.findById0(id);
                    })
                    .ifPresent(user -> {
                        permissionGrantService.deleteByUser(user);
                        repository.delete(user);
                    });
        } else {
            throw new ResourceNotFoundException(User.class, id);
        }
    }

    /**
     * @return how many users are in each state, and how many accounts in the Keycloak group of those pending approval
     *     are not users yet
     */
    @RequiresAdmin
    public UserStatisticsDto getStatistics() {
        final Map<String, Long> usersByState = repository.countByState().stream()
                .collect(Collectors.toMap(UserRepository.StateCount::getStateId, UserRepository.StateCount::getCount, Long::sum, TreeMap::new));
        final long pendingApproval = pendingApprovalGroup()
                .map(group -> groupMembers(group.getId()).stream()
                        .filter(representation -> !repository.existsByExternalId(representation.getId()))
                        .count())
                .orElse(0L);

        return new UserStatisticsDto(usersByState, pendingApproval);
    }

    /**
     * @param id the user id
     * @return the Spexregister client roles of the user's Keycloak account
     */
    @RequiresAdmin
    public Set<AuthorityDto> getAuthoritiesByUser(final Long id) {
        if (doesUserExist(id)) {
            return repository.findById0(id)
                    .flatMap(model -> findResourceByExternalId(model.getExternalId()))
                    .map(resource -> {
                        final List<RoleRepresentation> roleRepresentations = resource
                                .roles()
                                .clientLevel(keycloakClientId)
                                .listAll();

                        return extractRelevantAuthorities(roleRepresentations);
                    })
                    .orElseThrow(() -> new ExternalResourceNotFoundException(User.class, id));
        } else {
            throw new ResourceNotFoundException(User.class, id);
        }
    }

    /**
     * Gives the user's Keycloak account the client roles it lacks.
     *
     * @param userId the user id
     * @param ids    authority ids, such as {@code ADMIN}
     */
    @RequiresAdmin
    public void addAuthorities(final Long userId, final List<String> ids) {
        if (doUserAndAuthoritiesExist(userId, ids)) {
            repository.findById0(userId)
                    .map(permissionService::checkWritePermission)
                    .ifPresentOrElse(
                            user -> findResourceByExternalId(user.getExternalId()).ifPresent(resource -> {
                                final List<RoleRepresentation> currentRoles = resource
                                        .roles()
                                        .clientLevel(keycloakClientId)
                                        .listAll();

                                final List<String> currentRoleNames = currentRoles.stream()
                                        .map(RoleRepresentation::getName)
                                        .toList();

                                final List<RoleRepresentation> rolesToAdd = ids.stream()
                                        .filter(id -> !currentRoleNames.contains(id))
                                        .map(authorityService::getRoleRepresentationById)
                                        .toList();

                                if (!rolesToAdd.isEmpty()) {
                                    resource
                                            .roles()
                                            .clientLevel(keycloakClientId)
                                            .add(rolesToAdd);
                                    user.setLastModifiedAt(Instant.now());
                                    repository.save(user);
                                }
                            }),
                            () -> {
                                throw new ExternalResourceNotFoundException(User.class, userId);
                            });
        } else {
            throw new ResourcesNotFoundException(List.of(User.class, Authority.class), userId, String.join(",", ids));
        }
    }

    /**
     * Gives the user's Keycloak account the client role.
     *
     * @param userId the user id
     * @param id     the authority id, such as {@code ADMIN}
     */
    @RequiresAdmin
    public void addAuthority(final Long userId, final String id) {
        addAuthorities(userId, List.of(id));
    }

    /**
     * Takes the client roles from the user's Keycloak account.
     *
     * @param userId the user id
     * @param ids    authority ids, such as {@code ADMIN}
     */
    @RequiresAdmin
    public void removeAuthorities(final Long userId, final List<String> ids) {
        if (doUserAndAuthoritiesExist(userId, ids)) {
            repository.findById0(userId)
                    .map(permissionService::checkWritePermission)
                    .ifPresentOrElse(
                            user -> findResourceByExternalId(user.getExternalId()).ifPresent(resource -> {
                                final List<RoleRepresentation> currentRoles = resource
                                        .roles()
                                        .clientLevel(keycloakClientId)
                                        .listAll();

                                final List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                                        .filter(r -> ids.contains(r.getName()))
                                        .toList();

                                if (!rolesToRemove.isEmpty()) {
                                    resource
                                            .roles()
                                            .clientLevel(keycloakClientId)
                                            .remove(rolesToRemove);
                                    user.setLastModifiedAt(Instant.now());
                                    repository.save(user);
                                }
                            }),
                            () -> {
                                throw new ResourceNotFoundException(String.format("User %s does not exist in Keycloak", userId));
                            });
        } else {
            throw new ResourcesNotFoundException(List.of(User.class, Authority.class), userId, String.join(",", ids));
        }
    }

    /**
     * Takes the client role from the user's Keycloak account.
     *
     * @param userId the user id
     * @param id     the authority id, such as {@code ADMIN}
     */
    @RequiresAdmin
    public void removeAuthority(final Long userId, final String id) {
        removeAuthorities(userId, List.of(id));
    }

    /**
     * For batch loading.
     *
     * @param ids user ids
     * @return the state of each user, by user id
     */
    @RequiresAdmin
    public Map<Long, StateDto> getStatesByUsers(final Collection<Long> ids) {
        return findReadable(ids).stream()
                .filter(user -> user.getState() != null)
                .collect(Collectors.toMap(User::getId, user -> STATE_MAPPER.toDto(user.getState())));
    }

    /**
     * For batch loading.
     *
     * @param ids user ids
     * @return the spexare of those users that have one, by user id
     */
    @RequiresAdmin
    public Map<Long, SpexareDto> findSpexareByUsers(final Collection<Long> ids) {
        return findReadable(ids).stream()
                .filter(user -> user.getSpexare() != null)
                .collect(Collectors.toMap(User::getId, user -> SPEXARE_MAPPER.toDto(user.getSpexare())));
    }

    private List<User> findReadable(final Collection<Long> ids) {
        return repository.findAllById(ids).stream()
                .filter(permissionService::hasReadPermission)
                .toList();
    }

    /**
     * @param id the user id
     * @return the user's state
     */
    @RequiresAdmin
    public StateDto getStateByUser(final Long id) {
        return repository
                .findById0(id)
                .map(User::getState)
                .map(STATE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(User.class, id));
    }

    /**
     * Moves the user to the state, enabling or disabling the Keycloak account when the state's enabled flag differs.
     *
     * @param userId the user id
     * @param id     the state id
     */
    @RequiresAdmin
    public void setState(final Long userId, final String id) {
        if (doUserAndStateExist(userId, id)) {
            repository
                    .findById0(userId)
                    .map(permissionService::checkWritePermission)
                    .ifPresent(user -> stateRepository
                            .findById(id)
                            .ifPresent(state -> {
                                if (!user.getState().getEnabled().equals(state.getEnabled())) {
                                    findResourceByExternalId(user.getExternalId())
                                            .ifPresent(resource -> {
                                                final UserRepresentation representation = resource.toRepresentation();

                                                representation.setEnabled(state.getEnabled());
                                                resource.update(representation);
                                            });
                                }
                                user.setState(state);
                                repository.save(user);
                            })
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(User.class, State.class), userId, id);
        }
    }

    /**
     * @param id the user id
     * @return the user's spexare, or empty when there is none
     */
    @RequiresAdmin
    public Optional<SpexareDto> findSpexareByUser(final Long id) {
        if (doesUserExist(id)) {
            return repository
                    .findById0(id)
                    .map(User::getSpexare)
                    .map(SPEXARE_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(User.class, id);
        }
    }

    /**
     * Links the user to their spexare, and fills in the first and last name of the Keycloak account from it when
     * missing.
     *
     * @param userId the user id
     * @param id     the spexare id
     * @throws nu.fgv.register.server.util.error.ResourceAlreadyExistsException when the spexare belongs to another user
     */
    @RequiresAdmin
    public void addSpexare(final Long userId, final Long id) {
        if (doUserAndSpexareExist(userId, id)) {
            repository
                    .findById0(userId)
                    .map(permissionService::checkWritePermission)
                    .ifPresent(user -> spexareRepository
                            .findById(id)
                            .ifPresent(spexare -> {
                                if (spexare.getUser() != null && !spexare.getUser().equals(user)) {
                                    throw new ResourceAlreadyExistsException(Spexare.class, id);
                                }
                                user.setSpexare(spexare);
                                repository.save(user);
                                fillMissingNames(user.getExternalId(), spexare);
                            })
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(User.class, Spexare.class), userId, id);
        }
    }

    /**
     * Unlinks the user from their spexare.
     *
     * @param id the user id
     */
    @RequiresAdmin
    public void removeSpexare(final Long id) {
        if (doesUserExist(id)) {
            repository
                    .findById0(id)
                    .map(permissionService::checkWritePermission)
                    .filter(user -> user.getSpexare() != null)
                    .ifPresent(user -> {
                        user.setSpexare(null);
                        repository.save(user);
                    });
        } else {
            throw new ResourceNotFoundException(User.class, id);
        }
    }

    /**
     * Adds a user for each Keycloak account with a Spexregister role that is not one yet, removes the users whose
     * accounts are gone from Keycloak, and fills in missing names from linked spexare. Each user is synced in its own
     * transaction, so one failure does not stop the rest. Runs on the
     * {@code spexregister.jobs.sync-users.cron-expression} schedule.
     */
    @Scheduled(cron = "${spexregister.jobs.sync-users.cron-expression}")
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void scheduledSync() {
        log.info("Starting user sync job");
        runAsSystem(() -> {
            final Set<String> members = new HashSet<>();
            final Map<String, UserRepresentation> representations = new HashMap<>();
            final AtomicInteger synced = new AtomicInteger();
            final AtomicInteger removed = new AtomicInteger();
            final AtomicInteger named = new AtomicInteger();

            // Each user commits on its own, so one failure does not undo the rest of the run.
            authorityRepository.findAll().forEach(authority -> roleMembers(authority.getId()).forEach(representation -> {
                representations.putIfAbsent(representation.getId(), representation);
                if (members.add(representation.getId()) && !repository.existsByExternalId(representation.getId())) {
                    try {
                        inTransaction(() -> addSyncedUser(representation.getId()));
                        synced.incrementAndGet();
                    } catch (final Exception e) {
                        log.error("Could not sync user {} from Keycloak", representation.getId(), e);
                    }
                }
            }));

            repository.findAll().stream()
                    .filter(user -> !members.contains(user.getExternalId()))
                    .filter(user -> isDeletedInKeycloak(user.getExternalId()))
                    .forEach(user -> {
                        try {
                            inTransaction(() -> removeSyncedUser(user.getId()));
                            removed.incrementAndGet();
                        } catch (final Exception e) {
                            log.error("Could not remove user {} deleted in Keycloak", user.getExternalId(), e);
                        }
                    });

            repository.findAll().stream()
                    .filter(user -> user.getSpexare() != null)
                    .filter(user -> representations.containsKey(user.getExternalId()))
                    .filter(user -> hasMissingNames(representations.get(user.getExternalId())))
                    .forEach(user -> {
                        if (fillMissingNames(user.getExternalId(), user.getSpexare())) {
                            named.incrementAndGet();
                        }
                    });

            log.info("Finished user sync job (members: {}, synced: {}, removed: {}, named: {})", members.size(), synced.get(), removed.get(), named.get());
        });
    }

    // Keycloak returns role members a page at a time; asking once only ever yields the first page.
    private List<UserRepresentation> roleMembers(final String role) {
        final RoleResource roleResource = keycloakAdminClient
                .realm(keycloakRealm)
                .clients()
                .get(keycloakClientId)
                .roles()
                .get(role);

        return allPages(roleResource::getUserMembers);
    }

    private Optional<GroupRepresentation> pendingApprovalGroup() {
        try {
            return Optional.of(keycloakAdminClient.realm(keycloakRealm).getGroupByPath("/" + pendingApprovalGroup));
        } catch (final NotFoundException e) {
            log.warn("Group {} does not exist in Keycloak, users pending approval cannot be counted", pendingApprovalGroup);
            return Optional.empty();
        }
    }

    private List<UserRepresentation> groupMembers(final String groupId) {
        return allPages((first, max) -> keycloakAdminClient
                .realm(keycloakRealm)
                .groups()
                .group(groupId)
                .members(first, max));
    }

    private static List<UserRepresentation> allPages(final BiFunction<Integer, Integer, List<UserRepresentation>> fetch) {
        final List<UserRepresentation> all = new ArrayList<>();
        List<UserRepresentation> page;
        int first = 0;

        do {
            page = fetch.apply(first, SYNC_PAGE_SIZE);
            all.addAll(page);
            first += SYNC_PAGE_SIZE;
        } while (page.size() == SYNC_PAGE_SIZE);

        return all;
    }

    private void addSyncedUser(final String externalId) {
        final User user = repository.save(USER_MAPPER.toModel(externalId, getUserInitialState()));

        log.debug("Synced user {} from Keycloak", externalId);
    }

    private static boolean hasMissingNames(final UserRepresentation representation) {
        return !hasText(representation.getFirstName()) || !hasText(representation.getLastName());
    }

    private boolean fillMissingNames(final String externalId, final Spexare spexare) {
        try {
            final UserResource resource = keycloakAdminClient.realm(keycloakRealm).users().get(externalId);
            final UserRepresentation representation = resource.toRepresentation();

            if (!hasMissingNames(representation)) {
                return false;
            }
            if (!hasText(representation.getFirstName())) {
                representation.setFirstName(spexare.getFirstName());
            }
            if (!hasText(representation.getLastName())) {
                representation.setLastName(spexare.getLastName());
            }
            resource.update(representation);
            log.debug("Set missing names of user {} in Keycloak from spexare {}", externalId, spexare.getId());
            return true;
        } catch (final Exception e) {
            log.warn("Could not set names of user {} in Keycloak", externalId, e);
            return false;
        }
    }

    private boolean isDeletedInKeycloak(final String externalId) {
        try {
            keycloakAdminClient.realm(keycloakRealm).users().get(externalId).toRepresentation();
            return false;
        } catch (final NotFoundException _) {
            return true;
        } catch (final Exception e) {
            log.warn("Could not look up user {} in Keycloak, keeping it", externalId, e);
            return false;
        }
    }

    private void removeSyncedUser(final Long id) {
        repository.findById(id).ifPresent(user -> {
            permissionGrantService.deleteByUser(user);
            repository.delete(user);
            log.info("Removed user {} that no longer exists in Keycloak", user.getExternalId());
        });
    }

    private void inTransaction(final Runnable work) {
        final TransactionTemplate template = new TransactionTemplate(transactionManager);

        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(_ -> work.run());
    }

    private Optional<UserResource> findResourceByExternalId(final String externalId) {
        try {
            final UserResource userResource = keycloakAdminClient
                    .realm(keycloakRealm)
                    .users()
                    .get(externalId);

            return Optional.of(userResource);
        } catch (final Exception e) {
            log.error("Error while retrieving user info from Keycloak for external id {}", externalId, e);
            throw new InternalErrorException("Error while communicating with Keycloak");
        }
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

    private Set<AuthorityDto> extractRelevantAuthorities(final List<RoleRepresentation> roleRepresentations) {
        return authorityRepository.findAll().stream()
                .filter(authority -> roleRepresentations.stream().anyMatch(r -> r.getName().equals(authority.getId())))
                .collect(Collectors.collectingAndThen(Collectors.toSet(), AUTHORITY_MAPPER::toDtos));
    }

    private boolean doesUserExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

    private boolean doUserAndAuthoritiesExist(final Long userId, final List<String> authorityIds) {
        return doesUserExist(userId) && authorityIds.stream().allMatch(authorityRepository::existsById);
    }

    private boolean doUserAndStateExist(final Long userId, final String stateId) {
        return doesUserExist(userId) && stateRepository.existsById(stateId);
    }

    private boolean doUserAndSpexareExist(final Long userId, final Long spexareId) {
        return doesUserExist(userId) && spexareRepository.findById0(spexareId).isPresent();
    }

    private boolean doesUserWithEmailExist(final String email, @Nullable final String excludedExternalId) {
        final List<UserRepresentation> users = keycloakAdminClient.realm(keycloakRealm).users().searchByEmail(email, true);

        return users != null && users.stream()
                .anyMatch(user -> !user.getId().equals(excludedExternalId));

    }

    private State getUserInitialState() {
        return stateRepository
                .findByInitial(true)
                .orElseThrow(() -> new IllegalStateException("Initial state could not be found"));
    }

    private String generateTemporaryPassword() {
        final CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase, 2);
        final CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase, 2);
        final CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit, 2);
        final CharacterRule specialCharacterRule = new CharacterRule(new CharacterData() {
            public String getErrorCode() {
                return ERROR_CODE;
            }

            public String getCharacters() {
                return "!@#$%^&*()_+";
            }
        }, 2);
        final PasswordGenerator passwordGenerator = new PasswordGenerator(15, List.of(specialCharacterRule, lowerCaseRule, upperCaseRule, digitRule));

        return passwordGenerator.generate().toString();
    }
}
