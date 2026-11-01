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

package nu.fgv.register.server.spexare.toggle;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spexare.toggle.ToggleMapper.TOGGLE_MAPPER;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasId;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasSpexareIn;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasType;

/**
 * A spexare's toggles, one per type. Reading needs read access to the spexare and changing write access.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ToggleService {

    private final ToggleRepository repository;
    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;
    private final TypeService typeService;
    private final PermissionService permissionService;

    /**
     * @param ids spexare ids
     * @return the toggles of each spexare, by spexare id
     */
    @RequiresAdminOrEditorOrUser
    public Map<UUID, List<ToggleDto>> findBySpexareIds(final Collection<UUID> ids) {
        final List<Spexare> spexare = spexareRepository.findByUuidIn(ids).stream()
                .filter(permissionService::hasReadPermission)
                .toList();

        if (spexare.isEmpty()) {
            return Map.of();
        }

        return repository
                .findAll(hasSpexareIn(spexare), Sort.by(Toggle_.TYPE))
                .stream()
                .collect(Collectors.groupingBy(model -> model.getSpexare().getUuid(), Collectors.mapping(TOGGLE_MAPPER::toDto, Collectors.toList())));
    }

    /**
     * @param id the spexare id
     * @return the spexare's toggles
     */
    @RequiresAdminOrEditorOrUser
    public List<ToggleDto> findBySpexare(final UUID id) {
        return findBySpexare(id, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), Pageable.unpaged(Sort.by(Toggle_.TYPE)))
                                .stream()
                                .map(TOGGLE_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    /**
     * @param spexareId the spexare id
     * @param scroll    the window to read
     * @param sort      the order
     * @return the window with the total count
     */
    @RequiresAdminOrEditorOrUser
    public CountedWindow<ToggleDto> findBySpexare(final UUID spexareId, final ScrollRequest scroll, final Sort sort) {
        return findBySpexare(spexareId, spexare ->
                        repository
                                .findBy(hasSpexare(spexare), query -> {
                                    final long total = query.count();

                                    return CountedWindow.of(query
                                            .limit(scroll.limit())
                                            .sortBy(sort)
                                            .scroll(scroll.positionFor(total))
                                            .map(TOGGLE_MAPPER::toDto), total);
                                }),
                GraphqlUtil::emptyWindow
        );
    }

    /**
     * @param spexareId the spexare id
     * @param pageable  the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<ToggleDto> findBySpexare(final UUID spexareId, final Pageable pageable) {
        return findBySpexare(spexareId, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), pageable)
                                .map(TOGGLE_MAPPER::toDto),
                Page::empty
        );
    }

    /**
     * @param spexareId the spexare id
     * @param id        the toggle id
     * @return the toggle
     */
    @RequiresAdminOrEditorOrUser
    public ToggleDto findById(final UUID spexareId, final UUID id) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findByUuid(id))
                    .filter(toggle -> toggle.getSpexare().getUuid().equals(spexareId))
                    .map(TOGGLE_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Toggle.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Toggle.class), spexareId, id);
        }
    }

    /**
     * @param id the toggle id
     * @return whether the toggle exists
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final UUID id) {
        return repository
                .findByUuid(id)
                .isPresent();
    }

    /**
     * @param spexareId the spexare id
     * @param typeId    the toggle type, such as {@code CHALMERS_STUDENT}
     * @param dto       the value
     * @return the created toggle
     * @throws nu.fgv.register.server.util.error.SubresourceAlreadyExistsException when the spexare already has a toggle
     *                                                                             of the type
     */
    @RequiresAdminOrEditorOrUser
    public ToggleDto create(final UUID spexareId, final String typeId, final ToggleCreateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findByUuid0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> !repository.exists(hasSpexare(spexare).and(hasType(type))))
                            .map(spexare -> {
                                final Toggle toggle = new Toggle();
                                toggle.setSpexare(spexare);
                                toggle.setType(type);
                                toggle.setValue(dto.value());
                                return repository.save(toggle);
                            })
                    )
                    .map(TOGGLE_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(List.of(Spexare.class, Type.class, Toggle.class), Toggle_.TYPE, typeId, spexareId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class), spexareId, typeId);
        }
    }

    /**
     * @param spexareId the spexare id
     * @param typeId    the id of the toggle type, such as {@code CHALMERS_STUDENT}
     * @param id        the toggle id
     * @param dto       the new values
     * @return the updated toggle
     */
    @RequiresAdminOrEditorOrUser
    public ToggleDto update(final UUID spexareId, final String typeId, final UUID id, final ToggleUpdateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesToggleExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findByUuid0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findByUuid(id))
                            .filter(toggle -> toggle.getSpexare().getUuid().equals(spexareId))
                            .map(toggle -> {
                                toggle.setValue(dto.value());
                                return repository.save(toggle);
                            })
                            .map(TOGGLE_MAPPER::toDto)
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(Toggle.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class, Toggle.class), spexareId, typeId, id);
        }
    }

    /**
     * @param spexareId the spexare id
     * @param typeId    the id of the toggle type, such as {@code CHALMERS_STUDENT}
     * @param id        the toggle id
     */
    @RequiresAdminOrEditorOrUser
    public void deleteById(final UUID spexareId, final String typeId, final UUID id) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesToggleExist(id)) {
            typeRepository
                    .findById(typeId)
                    .ifPresent(type -> spexareRepository
                            .findByUuid0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findByUuid(id))
                            .filter(toggle -> toggle.getSpexare().getUuid().equals(spexareId))
                            .ifPresentOrElse(
                                    toggle -> repository.deleteById(toggle.getId()),
                                    () -> {
                                        throw new ResourceNotFoundException(Toggle.class, id);
                                    }
                            )
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class, Toggle.class), spexareId, typeId, id);
        }
    }

    private <T> T findBySpexare(final UUID id, final Function<Spexare, T> queryFunction, final Supplier<T> emptyResult) {
        if (doesSpexareExist(id)) {
            return spexareRepository
                    .findByUuid0(id)
                    .map(permissionService::checkReadPermission)
                    .map(queryFunction)
                    .orElseGet(emptyResult);
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    private boolean doesSpexareExist(final UUID id) {
        return spexareRepository.findByUuid0(id).isPresent();
    }

    private boolean doesToggleExist(final UUID id) {
        return repository.findByUuid(id).isPresent();
    }

    private boolean doSpexareAndTypeExist(final UUID spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeService.existsByIdAndType(typeId, TypeType.TOGGLE);
    }

}
