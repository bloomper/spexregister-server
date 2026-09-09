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

package nu.fgv.register.server.spexare.address;

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

import static nu.fgv.register.server.spexare.address.AddressMapper.ADDRESS_MAPPER;
import static nu.fgv.register.server.spexare.address.AddressSpecification.hasId;
import static nu.fgv.register.server.spexare.address.AddressSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.address.AddressSpecification.hasType;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AddressService {

    private final AddressRepository repository;
    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;
    private final TypeService typeService;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<AddressDto> findBySpexare(final Long id) {
        return findBySpexare(id, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), Pageable.unpaged(Sort.by(Address_.TYPE)))
                                .stream()
                                .map(ADDRESS_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    @RequiresAdminOrEditorOrUser
    public CountedWindow<AddressDto> findBySpexare(final Long id, final String filter, final ScrollRequest scroll, final Sort sort) {
        return findBySpexare(id, spexare ->
                        repository
                                .findBy(hasText(filter) ?
                                                SpecificationsBuilder.<Address>builder().build(FilterParser.parse(filter), AddressSpecification::new).and(hasSpexare(spexare)) :
                                                hasSpexare(spexare),
                                        query -> {
                                            final long total = query.count();

                                            return CountedWindow.of(query
                                                    .limit(scroll.limit())
                                                    .sortBy(sort)
                                                    .scroll(scroll.positionFor(total))
                                                    .map(ADDRESS_MAPPER::toDto), total);
                                        }),
                GraphqlUtil::emptyWindow
        );
    }

    @RequiresAdminOrEditorOrUser
    public Page<AddressDto> findBySpexare(final Long id, final String filter, final Pageable pageable) {
        return findBySpexare(id, spexare ->
                        hasText(filter) ?
                                repository
                                        .findAll(SpecificationsBuilder.<Address>builder().build(FilterParser.parse(filter), AddressSpecification::new).and(hasSpexare(spexare)), pageable)
                                        .map(ADDRESS_MAPPER::toDto) :
                                repository
                                        .findAll(hasSpexare(spexare), pageable)
                                        .map(ADDRESS_MAPPER::toDto),
                Page::empty
        );
    }

    @RequiresAdminOrEditorOrUser
    public AddressDto findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId) && doesAddressExist(id)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(address -> address.getSpexare().getId().equals(spexareId))
                    .map(ADDRESS_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Address.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Address.class), spexareId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById(id)
                .isPresent();
    }

    @RequiresAdminOrEditorOrUser
    public AddressDto create(final Long spexareId, final String typeId, final AddressCreateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> !repository.exists(hasSpexare(spexare).and(hasType(type))))
                            .map(spexare -> {
                                final Address address = ADDRESS_MAPPER.toModel(dto);
                                address.setSpexare(spexare);
                                address.setType(type);
                                return repository.save(address);
                            })
                    )
                    .map(ADDRESS_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(List.of(Spexare.class, Type.class, Address.class), Address_.TYPE, typeId, spexareId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class), spexareId, typeId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public AddressDto update(final Long spexareId, final String typeId, final Long id, final AddressUpdateDto dto) {
        return partialUpdate(spexareId, typeId, id, dto);
    }

    @RequiresAdminOrEditorOrUser
    public AddressDto partialUpdate(final Long spexareId, final String typeId, final Long id, final AddressUpdateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesAddressExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(address -> address.getSpexare().getId().equals(spexareId))
                            .map(address -> {
                                ADDRESS_MAPPER.toPartialModel(dto, address);
                                return address;
                            })
                            .map(repository::save)
                            .map(ADDRESS_MAPPER::toDto)
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(Address.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class, Address.class), spexareId, typeId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final String typeId, final Long id) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesAddressExist(id)) {
            typeRepository
                    .findById(typeId)
                    .ifPresent(type -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(address -> address.getSpexare().getId().equals(spexareId))
                            .ifPresentOrElse(
                                    address -> repository.deleteById(address.getId()),
                                    () -> {
                                        throw new ResourceNotFoundException(Address.class, id);
                                    }
                            )
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Type.class, Address.class), spexareId, typeId, id);
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

    private boolean doesAddressExist(final Long id) {
        return repository.findById(id).isPresent();
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeService.existsByIdAndType(typeId, TypeType.ADDRESS);
    }

}
