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

package nu.fgv.register.server.spexare;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.PageWithFacets;
import nu.fgv.register.server.util.search.PageWithFacetsImpl;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.SearchException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.AGGREGATIONS;
import static nu.fgv.register.server.spexare.SpexareSpecification.hasIds;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexareService {

    private final SpexareRepository repository;

    public PageWithFacets<SpexareDto> search(final String query, final Pageable pageable) {
        final SearchResult<Spexare> searchResult = repository.search(query, pageable);
        final List<Facet> facets = AGGREGATIONS.stream()
                .filter(a -> {
                    try {
                        searchResult.aggregation(AggregationKey.of(a));
                        return true;
                    } catch (final SearchException e) {
                        return false;
                    }
                })
                .map(a -> Facet.builder()
                        .name(a)
                        .values(searchResult.aggregation(AggregationKey.of(a)))
                        .build())
                .toList();

        return new PageWithFacetsImpl<>(SPEXARE_MAPPER.toDtos(searchResult.hits()), pageable, searchResult.total(), facets);
    }

    public List<SpexareDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .toList();
    }

    public Page<SpexareDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Spexare>builder().build(FilterParser.parse(filter), SpexareSpecification::new), pageable)
                        .map(SPEXARE_MAPPER::toDto) :
                repository
                        .findAll(pageable)
                        .map(SPEXARE_MAPPER::toDto);
    }

    public SpexareDto findById(final Long id) {
        return repository
                .findById(id)
                .map(SPEXARE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    public List<SpexareDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .toList();
    }

    public SpexareDto create(final SpexareCreateDto dto) {
        return Optional.of(SPEXARE_MAPPER.toModel(dto))
                .map(model -> {
                    repository.save(model);

                    return SPEXARE_MAPPER.toDto(model);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create spexare"));
    }

    public SpexareDto update(final SpexareUpdateDto dto) {
        return partialUpdate(dto);
    }

    public SpexareDto partialUpdate(final SpexareUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(spexare -> {
                    SPEXARE_MAPPER.toPartialModel(dto, spexare);
                    return spexare;
                })
                .map(repository::save)
                .map(SPEXARE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, dto.getId()));
    }

    public void deleteById(final Long id) {
        if (doesSpexareExist(id)) {
            repository.deleteById(id);
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    public SpexareDto saveImage(final Long id, final byte[] image, @Nullable final String contentType) {
        return repository
                .findById0(id)
                .map(spexare -> {
                    spexare.setImage(image);
                    spexare.setImageContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(image));
                    repository.save(spexare);
                    return SPEXARE_MAPPER.toDto(spexare);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    public SpexareDto deleteImage(final Long id) {
        return repository
                .findById0(id)
                .map(spexare -> {
                    spexare.setImage(null);
                    spexare.setImageContentType(null);
                    repository.save(spexare);
                    return SPEXARE_MAPPER.toDto(spexare);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    public Pair<byte[], String> getImage(final Long id) {
        if (!doesSpexareExist(id)) {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
        return repository
                .findById(id)
                .filter(spexare -> spexare.getImage() != null && hasText(spexare.getImageContentType()))
                .map(spexare -> Pair.of(spexare.getImage(), spexare.getImageContentType()))
                .orElseThrow(() -> new ResourceNoValueException(Spexare.class, Spexare_.IMAGE, id));
    }

    public SpexareDto findPartnerBySpexare(final Long id) {
        if (doesSpexareExist(id)) {
            return repository
                    .findById(id)
                    .filter(spexare -> spexare.getPartner() != null)
                    .map(Spexare::getPartner)
                    .map(SPEXARE_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNoValueException(Spexare.class, Spexare_.PARTNER, id));
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    public void updatePartner(final Long spexareId, final Long id) {
        if (doSpexareAndPartnerExist(spexareId, id)) {
            repository
                    .findById0(spexareId)
                    .ifPresent(spexare -> repository
                            .findById0(id)
                            .ifPresent(partner -> {
                                spexare.setPartner(partner);
                                partner.setPartner(spexare);
                                repository.save(spexare);
                                repository.save(partner);
                            }));
        } else {
            throw new ResourcesNotFoundException(new String[]{Spexare.class.getSimpleName(), "Partner"}, spexareId, id);
        }
    }

    public void deletePartner(final Long id) {
        if (doesSpexareExist(id)) {
            repository
                    .findById0(id)
                    .ifPresent(spexare -> {
                        final Spexare partner = spexare.getPartner();

                        spexare.setPartner(null);
                        repository.save(spexare);

                        if (partner != null) {
                            partner.setPartner(null);
                            repository.save(partner);
                        }
                    });
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

    private boolean doSpexareAndPartnerExist(final Long spexareId, final Long partnerId) {
        return doesSpexareExist(spexareId) && doesSpexareExist(partnerId);
    }

}
