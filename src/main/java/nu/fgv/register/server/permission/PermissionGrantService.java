/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.permission;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.security.policy.Caller;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdmin;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

import static nu.fgv.register.server.permission.PermissionGrantMapper.PERMISSION_GRANT_MAPPER;
import static nu.fgv.register.server.permission.PermissionGrantSpecification.NO_FILTER;
import static org.springframework.util.StringUtils.hasText;

/**
 * Administers permission grants.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Service
@Transactional
public class PermissionGrantService {

    private static final Set<String> GRANTABLE_ROLES = Set.of(Caller.USER, Caller.EDITOR, Caller.ADMIN);

    private final PermissionGrantRepository repository;
    private final UserRepository userRepository;
    private final SpexareRepository spexareRepository;
    private final SpexRepository spexRepository;

    /**
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresAdmin
    public CountedWindow<PermissionGrantDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<PermissionGrant>builder().build(FilterParser.parse(filter), PermissionGrantSpecification::new) :
                                NO_FILTER,
                        query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(PERMISSION_GRANT_MAPPER::toDto), total);
                        });
    }

    @RequiresAdmin
    public PermissionGrantDto findById(final Long id) {
        return repository
                .findById(id)
                .map(PERMISSION_GRANT_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(PermissionGrant.class, id));
    }

    /**
     * Creates a grant to exactly one of a user and a grantable role ({@code USER}, {@code EDITOR} or {@code ADMIN}). A
     * {@link PermissionGrantScope#SPEXARE} grant names a spexare and no spex; the other scopes name a spex and no
     * spexare.
     *
     * @param dto the grant
     * @return the created grant
     * @throws nu.fgv.register.server.util.error.BadRequestException when the grant breaks these rules, names an unknown
     *     user, spexare or spex, or ends in the past
     */
    @RequiresAdmin
    public PermissionGrantDto create(final PermissionGrantCreateDto dto) {
        if ((dto.userId() == null) == (dto.role() == null)) {
            throw new BadRequestException("A grant names exactly one of a user and a role");
        }
        if (dto.role() != null && !GRANTABLE_ROLES.contains(dto.role())) {
            throw new BadRequestException("Role %s cannot be granted rights".formatted(dto.role()));
        }
        requireNotInThePast(dto.validUntil());

        final PermissionGrant grant = new PermissionGrant();

        grant.setUser(dto.userId() == null ? null : userRepository.findById(dto.userId())
                .orElseThrow(() -> new BadRequestException("Unknown user %d".formatted(dto.userId()))));
        grant.setRole(dto.role());
        grant.setPermission(dto.permission());
        grant.setScope(dto.scope());
        grant.setValidUntil(dto.validUntil());

        if (dto.scope() == PermissionGrantScope.SPEXARE) {
            if (dto.spexareId() == null || dto.spexId() != null) {
                throw new BadRequestException("A grant on one spexare names the spexare and no spex");
            }
            grant.setSpexare(spexareRepository.findById(dto.spexareId())
                    .orElseThrow(() -> new BadRequestException("Unknown spexare %d".formatted(dto.spexareId()))));
        } else {
            if (dto.spexId() == null || dto.spexareId() != null) {
                throw new BadRequestException("A grant on a spex names the spex and no spexare");
            }
            grant.setSpex(spexRepository.findById(dto.spexId())
                    .orElseThrow(() -> new BadRequestException("Unknown spex %d".formatted(dto.spexId()))));
        }

        return PERMISSION_GRANT_MAPPER.toDto(repository.save(grant));
    }

    /**
     * Changes only the permission and the last valid day. Who and what a grant applies to are fixed.
     *
     * @param dto the grant's id, permission and last valid day
     * @return the updated grant
     * @throws nu.fgv.register.server.util.error.BadRequestException when it would end in the past
     */
    @RequiresAdmin
    public PermissionGrantDto update(final PermissionGrantUpdateDto dto) {
        requireNotInThePast(dto.validUntil());

        return repository
                .findById(dto.id())
                .map(grant -> {
                    grant.setPermission(dto.permission());
                    grant.setValidUntil(dto.validUntil());
                    return repository.save(grant);
                })
                .map(PERMISSION_GRANT_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(PermissionGrant.class, dto.id()));
    }

    @RequiresAdmin
    public void deleteById(final Long id) {
        final PermissionGrant grant = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PermissionGrant.class, id));

        repository.delete(grant);
    }

    /**
     * Removes the grants on the spexare, before it is removed.
     *
     * @param spexare the spexare being removed
     */
    public void deleteBySpexare(final Spexare spexare) {
        repository.deleteAll(repository.findBySpexareId(spexare.getId()));
    }

    /**
     * Removes the grants on the spex, before it is removed.
     *
     * @param spex the spex being removed
     */
    public void deleteBySpex(final Spex spex) {
        repository.deleteAll(repository.findBySpexId(spex.getId()));
    }

    /**
     * Removes the grants to the user, before the user is removed.
     *
     * @param user the user being removed
     */
    public void deleteByUser(final User user) {
        repository.deleteAll(repository.findByUserId(user.getId()));
    }

    private static void requireNotInThePast(final @Nullable LocalDate validUntil) {
        if (validUntil != null && validUntil.isBefore(LocalDate.now())) {
            throw new BadRequestException("A grant cannot end in the past");
        }
    }
}
