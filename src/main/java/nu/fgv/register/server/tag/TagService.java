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

package nu.fgv.register.server.tag;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresReferenceDataReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import nu.fgv.register.server.security.policy.Permission;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;
import static nu.fgv.register.server.tag.TagSpecification.NO_FILTER;
import static nu.fgv.register.server.tag.TagSpecification.hasIds;
import static org.springframework.util.StringUtils.hasText;

/**
 * Tags. Editors change and remove them.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TagService {

    private final TagRepository repository;
    private final PermissionService permissionService;

    /**
     * The tags the caller may read that match the filter.
     *
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresReferenceDataReader
    public CountedWindow<TagDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<Tag>builder().build(FilterParser.parse(filter), TagSpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(TAG_MAPPER::toDto), total);
                        });
    }

    /**
     * The tags the caller may read that match the filter, for the REST API.
     *
     * @param filter   a filter expression, or blank for all
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<TagDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Tag>builder().build(FilterParser.parse(filter), TagSpecification::new), pageable, Permission.READ)
                        .map(TAG_MAPPER::toDto) :
                repository
                        .findAll(pageable, Permission.READ)
                        .map(TAG_MAPPER::toDto);
    }

    /**
     * @param id the tag id
     * @return the tag
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such tag
     */
    @RequiresReferenceDataReader
    public TagDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(TAG_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Tag.class, id));
    }

    /**
     * @param id the tag id
     * @return whether the tag exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    /**
     * The tags to export, streamed when iterated, so it must be consumed inside a transaction.
     *
     * @param ids    the tags to export, or empty to use the filter
     * @param filter a filter expression when no ids are given, or blank for all
     * @param sort   the order
     * @return the tags the caller may read
     */
    @RequiresAdminOrEditorOrUser
    public Iterable<Tag> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<Tag> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<Tag>builder()
                        .build(FilterParser.parse(filter), TagSpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * @param dto the new tag
     * @return the created tag
     */
    @RequiresAdminOrEditor
    public TagDto create(final TagCreateDto dto) {
        return Optional.of(TAG_MAPPER.toModel(dto))
                .map(model -> {
                    final Tag tag = repository.save(model);

                    return TAG_MAPPER.toDto(tag);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create tag"));
    }

    /**
     * The same as {@link #partialUpdate}.
     *
     * @param dto the tag id and new values
     * @return the updated tag
     */
    @RequiresAdminOrEditor
    public TagDto update(final TagUpdateDto dto) {
        return partialUpdate(dto);
    }

    /**
     * Changes the values given; null values are left unchanged.
     *
     * @param dto the tag id and new values
     * @return the updated tag
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such tag
     */
    @RequiresAdminOrEditor
    public TagDto partialUpdate(final TagUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(tag -> {
                    TAG_MAPPER.toPartialModel(dto, tag);
                    return tag;
                })
                .map(repository::save)
                .map(TAG_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Tag.class, dto.id()));
    }

    /**
     * @param id the tag id
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such tag
     */
    @RequiresAdminOrEditor
    public void deleteById(final Long id) {
        if (doesTagExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(tag -> {
                        repository.delete(tag);
                    });
        } else {
            throw new ResourceNotFoundException(Tag.class, id);
        }
    }

    private boolean doesTagExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

}
