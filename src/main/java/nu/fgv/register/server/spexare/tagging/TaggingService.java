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

package nu.fgv.register.server.spexare.tagging;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagDto;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.tag.Tag_;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static nu.fgv.register.server.spexare.tagging.TaggingSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.tagging.TaggingSpecification.hasSpexareId;
import static nu.fgv.register.server.spexare.tagging.TaggingSpecification.hasTag;
import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaggingService {

    private final TagRepository tagRepository;
    private final SpexareRepository spexareRepository;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<TagDto> findBySpexare(final Long id) {
        return findBySpexare(id, () ->
                tagRepository
                        .findAll(hasSpexareId(id), Pageable.unpaged(Sort.by(Tag_.NAME)))
                        .stream()
                        .map(TAG_MAPPER::toDto)
                        .toList()
        );
    }

    @RequiresAdminOrEditorOrUser
    public Window<TagDto> findBySpexare(final Long spexareId, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return findBySpexare(spexareId, () -> tagRepository
                .findBy(hasSpexareId(spexareId), query -> query
                        .limit(limit)
                        .sortBy(sort)
                        .scroll(scrollPosition))
                .map(TAG_MAPPER::toDto));
    }

    @RequiresAdminOrEditorOrUser
    public Page<TagDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        return findBySpexare(spexareId, () -> tagRepository
                .findAll(hasSpexareId(spexareId), pageable)
                .map(TAG_MAPPER::toDto));
    }

    @RequiresAdminOrEditorOrUser
    public void create(final Long spexareId, final Long tagId) {
        if (doSpexareAndTagExist(spexareId, tagId)) {
            tagRepository
                    .findById(tagId)
                    .ifPresent(tag -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> !spexareRepository.exists(hasSpexare(spexare).and(hasTag(tag))))
                            .ifPresentOrElse(
                                    spexare -> {
                                        if (spexare.getTags() == null) {
                                            spexare.setTags(new java.util.HashSet<>());
                                        }
                                        spexare.getTags().add(tag);
                                        spexare.setLastModifiedAt(Instant.now());
                                        spexareRepository.save(spexare);
                                    },
                                    () -> {
                                        throw new SubresourceAlreadyExistsException(List.of(Spexare.class, Tag.class), Tag_.ID, tagId, spexareId);
                                    })
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Tag.class), spexareId, tagId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final Long tagId) {
        if (doSpexareAndTagExist(spexareId, tagId)) {
            tagRepository
                    .findById(tagId)
                    .ifPresent(tag -> spexareRepository
                            .findById0(spexareId)
                            .map(permissionService::checkWritePermission)
                            .filter(spexare -> spexareRepository.exists(hasSpexare(spexare).and(hasTag(tag))))
                            .ifPresentOrElse(
                                    spexare -> {
                                        if (spexare.getTags() == null) {
                                            spexare.setTags(new java.util.HashSet<>());
                                        }
                                        spexare.getTags().remove(tag);
                                        if (spexare.getTags().isEmpty()) {
                                            spexare.setTags(null);
                                        }
                                        spexare.setLastModifiedAt(Instant.now());
                                        spexareRepository.save(spexare);
                                    },
                                    () -> {
                                        throw new ResourcesNotFoundException(List.of(Spexare.class, Tag.class), spexareId, tagId);
                                    }
                            )
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Tag.class), spexareId, tagId);
        }
    }

    private <T> T findBySpexare(final Long spexareId, final Supplier<T> querySupplier) {
        if (isAllowedToReadSpexare(spexareId)) {
            return querySupplier.get();
        } else {
            throw new ResourceNotFoundException(Spexare.class, spexareId);
        }
    }

    private boolean isAllowedToReadSpexare(final Long spexareId) {
        return spexareRepository.findById0(spexareId).map(permissionService::checkReadPermission).isPresent();
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doSpexareAndTagExist(final Long spexareId, final Long tagId) {
        return doesSpexareExist(spexareId) && tagRepository.existsById(tagId);
    }

}
