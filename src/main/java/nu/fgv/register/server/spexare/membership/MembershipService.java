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

package nu.fgv.register.server.spexare.membership;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static nu.fgv.register.server.spexare.membership.MembershipMapper.MEMBERSHIP_MAPPER;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasId;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasType;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasYear;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class MembershipService {

    private final MembershipRepository repository;
    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;
    private final TypeService typeService;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<MembershipDto> findBySpexare(final Long id) {
        return findBySpexare(id, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), Pageable.unpaged(Sort.by(Membership_.TYPE)))
                                .stream()
                                .map(MEMBERSHIP_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    @RequiresAdminOrEditorOrUser
    public Window<MembershipDto> findBySpexare(final Long spexareId, final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return findBySpexare(spexareId, spexare ->
                        hasText(filter) ?
                                repository
                                        .findBy(SpecificationsBuilder.<Membership>builder().build(FilterParser.parse(filter), MembershipSpecification::new).and(hasSpexare(spexare)), query -> query
                                                .limit(limit)
                                                .sortBy(sort)
                                                .scroll(scrollPosition))
                                        .map(MEMBERSHIP_MAPPER::toDto) :
                                repository
                                        .findBy(hasSpexare(spexare), query -> query
                                                .limit(limit)
                                                .sortBy(sort)
                                                .scroll(scrollPosition))
                                        .map(MEMBERSHIP_MAPPER::toDto),
                GraphqlUtil::emptyWindow
        );
    }

    @RequiresAdminOrEditorOrUser
    public Page<MembershipDto> findBySpexare(final Long spexareId, final String filter, final Pageable pageable) {
        return findBySpexare(spexareId, spexare ->
                        hasText(filter) ?
                                repository
                                        .findAll(SpecificationsBuilder.<Membership>builder().build(FilterParser.parse(filter), MembershipSpecification::new).and(hasSpexare(spexare)), pageable)
                                        .map(MEMBERSHIP_MAPPER::toDto) :
                                repository
                                        .findAll(hasSpexare(spexare), pageable)
                                        .map(MEMBERSHIP_MAPPER::toDto),
                Page::empty
        );
    }

    @RequiresAdminOrEditorOrUser
    public MembershipDto findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(membership -> membership.getSpexare().getId().equals(spexareId))
                    .map(MEMBERSHIP_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Membership.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Membership.class), spexareId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public MembershipDto create(final Long spexareId, final String typeId, final MembershipCreateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> !repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasYear(dto.year()))))
                            .map(spexare -> {
                                final Membership membership = new Membership();
                                membership.setSpexare(spexare);
                                membership.setType(type);
                                membership.setYear(dto.year());
                                return repository.save(membership);
                            })
                    )
                    .map(MEMBERSHIP_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(List.of(Spexare.class, Type.class, Membership.class), Membership_.YEAR, dto.year(), spexareId, typeId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class), spexareId, typeId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final String typeId, final Long id) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesMembershipExist(id)) {
            typeRepository
                    .findById(typeId)
                    .ifPresent(type -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(membership -> membership.getSpexare().getId().equals(spexareId))
                            .ifPresentOrElse(
                                    membership -> repository.deleteById(membership.getId()),
                                    () -> {
                                        throw new ResourceNotFoundException(Membership.class, id);
                                    }
                            )
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class, Membership.class), spexareId, typeId, id);
        }
    }

    private <T> T findBySpexare(final Long id, final Function<Spexare, T> queryFunction, final Supplier<T> emptyResult) {
        if (doesSpexareExist(id)) {
            return spexareRepository
                    .findById0(id)
                    .map(permissionService::checkReadPermission)
                    .map(queryFunction)
                    .orElseGet(emptyResult);
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doesMembershipExist(final Long id) {
        return repository.findById(id).isPresent();
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeService.existsByIdAndType(typeId, TypeType.MEMBERSHIP);
    }

}
