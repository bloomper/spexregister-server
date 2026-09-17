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

package nu.fgv.register.server.savedsearch;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceAlreadyExistsException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.savedsearch.SavedSearchMapper.SAVED_SEARCH_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserSubClaim;

/**
 * Every lookup is scoped to the current user, so a saved search is only ever visible to its owner.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SavedSearchService {

    private final SavedSearchRepository repository;

    @RequiresAdminOrEditorOrUser
    public List<SavedSearchDto> findAll() {
        return repository
                .findByOwnerExternalIdOrderByNameAsc(getCurrentUserSubClaim())
                .stream()
                .map(SAVED_SEARCH_MAPPER::toDto)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public SavedSearchDto findById(final Long id) {
        return findOwnedById(id)
                .map(SAVED_SEARCH_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(SavedSearch.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public SavedSearchDto create(final SavedSearchCreateDto dto) {
        final String owner = getCurrentUserSubClaim();

        if (repository.existsByOwnerExternalIdAndNameIgnoreCase(owner, dto.name())) {
            throw new ResourceAlreadyExistsException(SavedSearch.class, dto.name());
        }

        return Optional.of(SAVED_SEARCH_MAPPER.toModel(dto))
                .map(model -> {
                    model.setOwnerExternalId(owner);

                    return SAVED_SEARCH_MAPPER.toDto(repository.save(model));
                })
                .orElseThrow(() -> new InternalErrorException("Could not create saved search"));
    }

    @RequiresAdminOrEditorOrUser
    public SavedSearchDto update(final SavedSearchUpdateDto dto) {
        final String owner = getCurrentUserSubClaim();

        return findOwnedById(dto.id())
                .map(model -> {
                    if (!model.getName().equalsIgnoreCase(dto.name())
                            && repository.existsByOwnerExternalIdAndNameIgnoreCase(owner, dto.name())) {
                        throw new ResourceAlreadyExistsException(SavedSearch.class, dto.name());
                    }

                    SAVED_SEARCH_MAPPER.toPartialModel(dto, model);

                    return SAVED_SEARCH_MAPPER.toDto(repository.save(model));
                })
                .orElseThrow(() -> new ResourceNotFoundException(SavedSearch.class, dto.id()));
    }

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long id) {
        repository.delete(findOwnedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SavedSearch.class, id)));
    }

    private Optional<SavedSearch> findOwnedById(final Long id) {
        return repository.findByIdAndOwnerExternalId(id, getCurrentUserSubClaim());
    }
}
