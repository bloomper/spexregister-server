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
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static nu.fgv.register.server.spexare.toggle.ToggleMapper.TOGGLE_MAPPER;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasId;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasType;

/**
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

    @RequiresAdminOrEditorOrUser
    public List<ToggleDto> findBySpexare(final Long id) {
        return findBySpexare(id, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), Pageable.unpaged(Sort.by(Toggle_.TYPE)))
                                .stream()
                                .map(TOGGLE_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    @RequiresAdminOrEditorOrUser
    public CountedWindow<ToggleDto> findBySpexare(final Long spexareId, final ScrollRequest scroll, final Sort sort) {
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

    @RequiresAdminOrEditorOrUser
    public Page<ToggleDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        return findBySpexare(spexareId, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), pageable)
                                .map(TOGGLE_MAPPER::toDto),
                Page::empty
        );
    }

    @RequiresAdminOrEditorOrUser
    public ToggleDto findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(toggle -> toggle.getSpexare().getId().equals(spexareId))
                    .map(TOGGLE_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Toggle.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Toggle.class), spexareId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById(id)
                .isPresent();
    }

    @RequiresAdminOrEditorOrUser
    public ToggleDto create(final Long spexareId, final String typeId, final ToggleCreateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById0(spexareId)
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

    @RequiresAdminOrEditorOrUser
    public ToggleDto update(final Long spexareId, final String typeId, final Long id, final ToggleUpdateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesToggleExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(toggle -> toggle.getSpexare().getId().equals(spexareId))
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

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final String typeId, final Long id) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesToggleExist(id)) {
            typeRepository
                    .findById(typeId)
                    .ifPresent(type -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(toggle -> toggle.getSpexare().getId().equals(spexareId))
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

    private boolean doesToggleExist(final Long id) {
        return repository.findById(id).isPresent();
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeService.existsByIdAndType(typeId, TypeType.TOGGLE);
    }

}
