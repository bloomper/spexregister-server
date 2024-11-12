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

package nu.fgv.register.server.spexare.tag;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagDto;
import nu.fgv.register.server.tag.Tag_;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

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

    private final TaggingRepository repository;

    private final SpexareRepository spexareRepository;

    public Page<TagDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findBySpexareId(spexareId, pageable)
                    .map(TAG_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(Spexare.class, spexareId);
        }
    }

    public void create(final Long spexareId, final Long id) {
        if (doSpexareAndTagExist(spexareId, id)) {
            repository
                    .findById(id)
                    .ifPresent(tag -> spexareRepository
                            .findById0(spexareId)
                            .filter(spexare -> !repository.existsBySpexareIdAndTagId(spexare.getId(), tag.getId()))
                            .ifPresentOrElse(
                                    spexare -> {
                                        if (spexare.getTags() == null) {
                                            spexare.setTags(new java.util.HashSet<>());
                                        }
                                        spexare.getTags().add(tag);
                                        spexareRepository.save(spexare);
                                    },
                                    () -> {
                                        throw new SubresourceAlreadyExistsException(List.of(Spexare.class, Tag.class), Tag_.ID, id, spexareId);
                                    })
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Tag.class), spexareId, id);
        }
    }

    public void deleteById(final Long spexareId, final Long id) {
        if (doSpexareAndTagExist(spexareId, id)) {
            repository
                    .findById(id)
                    .ifPresent(tag -> spexareRepository
                            .findById0(spexareId)
                            .filter(spexare -> repository.existsBySpexareIdAndTagId(spexare.getId(), tag.getId()))
                            .ifPresentOrElse(
                                    spexare -> {
                                        if (spexare.getTags() == null) {
                                            spexare.setTags(new java.util.HashSet<>());
                                        }
                                        spexare.getTags().remove(tag);
                                        if (spexare.getTags().isEmpty()) {
                                            spexare.setTags(null);
                                        }
                                        spexareRepository.save(spexare);
                                    },
                                    () -> {
                                        throw new ResourceNotFoundException(Tag.class, id);
                                    }
                            )
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Tag.class), spexareId, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doSpexareAndTagExist(final Long spexareId, final Long tagId) {
        return doesSpexareExist(spexareId) && repository.existsById(tagId);
    }

}
