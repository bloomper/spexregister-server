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
import nu.fgv.register.server.permission.PermissionGrantService;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.image.Image;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterCriteria;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.search.AggregationFilter;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.FacetGroup;
import nu.fgv.register.server.util.search.FacetValue;
import nu.fgv.register.server.util.search.PageWithFacets;
import nu.fgv.register.server.util.search.PageWithFacetsImpl;
import nu.fgv.register.server.util.search.WindowWithFacets;
import nu.fgv.register.server.util.search.WindowWithFacetsImpl;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrRevival;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUserOrRevival;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.SearchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import nu.fgv.register.server.security.policy.Permission;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.AGGREGATIONS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.AGGREGATION_DATA_QUALITY;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_COUNTRIES;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_MEMBERSHIPS;
import static nu.fgv.register.server.spexare.SpexareSpecification.NO_FILTER;
import static nu.fgv.register.server.spexare.SpexareSpecification.hasIds;
import static nu.fgv.register.server.spexare.SpexareSpecification.hasSpexActivityInSpexOrRevivals;
import static nu.fgv.register.server.util.Constants.AGGREGATION_COMPOSITE_DELIMITER;
import static nu.fgv.register.server.util.Constants.AGGREGATION_HIERARCHICAL_MARKER;
import static nu.fgv.register.server.util.FileUtil.requireSupportedImageMimeType;
import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserSubClaim;
import static nu.fgv.register.server.util.security.SecurityUtil.isAdministratorOrEditor;
import static nu.fgv.register.server.util.security.SecurityUtil.isRevival;
import static org.springframework.util.StringUtils.hasText;

/**
 * Spexare, their images and partners, and the search. What a caller sees is limited by
 * {@link nu.fgv.register.server.security.policy.SpexarePolicy}, and the social security number by
 * {@link SpexareSensitiveData}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexareService {

    public static final int MAX_IDS = 500;

    private final SpexareRepository repository;
    private final SpexRepository spexRepository;
    private final PermissionService permissionService;
    private final PermissionGrantService permissionGrantService;
    private final MessageSource messageSource;

    /**
     * Free-text search with facets. Facets the caller may not use are left out: published for those who see only
     * published spexare, and memberships for a caller limited to the revival service.
     *
     * @param query              the search text, blank for all
     * @param aggregationFilters facet values to narrow the search to
     * @param scroll             the window to read
     * @param sort               the order, by relevance when unsorted
     * @return the window with the total count and the facets
     * @throws nu.fgv.register.server.util.error.BadRequestException when a caller limited to the revival service
     *     narrows on memberships
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public WindowWithFacets<SpexareDto> search(final String query, final List<AggregationFilter> aggregationFilters, final ScrollRequest scroll, final Sort sort) {
        requirePermittedAggregationFilters(aggregationFilters);

        final int limit = scroll.limit();
        final long knownTotal = scroll.lastPage() ? repository.search(query, aggregationFilters, 0, 1, sort).total().hitCount() : 0L;
        final int offset = Math.toIntExact(scroll.offsetFor(knownTotal));

        final SearchResult<Spexare> searchResult = repository.search(query, aggregationFilters, offset, limit, sort);
        final List<Facet> facets = getFacets(searchResult);
        final long total = searchResult.total().hitCount();

        return new WindowWithFacetsImpl<>(SPEXARE_MAPPER.toDtos(searchResult.hits()), index -> ScrollPosition.offset(offset + index), total > offset + limit, facets, total);
    }

    /**
     * Free-text search with facets, for the REST API.
     *
     * @param query              the search text, blank for all
     * @param aggregationFilters facet values to narrow the search to
     * @param pageable           the page to read
     * @return the page with the facets
     */
    @RequiresAdminOrEditorOrUser
    public PageWithFacets<SpexareDto> search(final String query, final List<AggregationFilter> aggregationFilters, final Pageable pageable) {
        final SearchResult<Spexare> searchResult = repository.search(query, aggregationFilters, pageable);
        final List<Facet> facets = getFacets(searchResult);

        return new PageWithFacetsImpl<>(SPEXARE_MAPPER.toDtos(searchResult.hits()), pageable, searchResult.total(), facets);
    }

    /**
     * @return every facet with its value counts over all the spexare the caller may see
     */
    @RequiresAdminOrEditorOrUser
    public List<Facet> facets() {
        return getFacets(repository.search("", Collections.emptyList(), 0, 1, Sort.unsorted()));
    }

    /**
     * @param issue what the records lack
     * @return how many of the spexare the caller may see lack it
     */
    @RequiresAdminOrEditorOrUser
    public long countByDataQualityIssue(final DataQualityIssue issue) {
        return repository.countByDataQualityIssue(issue);
    }

    /**
     * @param filter a filter expression, or blank for all; the social security number only for those who may see it
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     * @throws nu.fgv.register.server.util.error.BadRequestException when the filter reaches what the caller may not
     *     filter on
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public CountedWindow<SpexareDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<Spexare>builder().build(parseFilter(filter), SpexareSpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(SPEXARE_MAPPER::toDto), total);
                        });
    }

    /**
     * @param filter   a filter expression, or blank for all
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<SpexareDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Spexare>builder().build(parseFilter(filter), SpexareSpecification::new), pageable, Permission.READ)
                        .map(SPEXARE_MAPPER::toDto) :
                repository
                        .findAll(pageable, Permission.READ)
                        .map(SPEXARE_MAPPER::toDto);
    }

    /**
     * @param ids at most {@value #MAX_IDS} spexare ids
     * @return those of the spexare the caller may read, unordered; ids of others are left out
     * @throws nu.fgv.register.server.util.error.BadRequestException for more than {@value #MAX_IDS} ids
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public List<SpexareDto> findByIds(final Collection<Long> ids) {
        if (ids.size() > MAX_IDS) {
            throw new BadRequestException("At most %d ids can be fetched at once".formatted(MAX_IDS));
        }
        if (ids.isEmpty()) {
            return List.of();
        }

        return repository
                .findAll(hasIds(List.copyOf(new LinkedHashSet<>(ids))), Permission.READ)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .toList();
    }

    /**
     * @param spexId an original spex id
     * @return the spexare with a spex activity in the spex or one of its revivals that the caller may read, by last
     *     name and first name
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public List<SpexareDto> findBySpex(final Long spexId) {
        final Spex spex = spexRepository
                .findById0(spexId)
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, spexId));

        return repository
                .findAll(hasSpexActivityInSpexOrRevivals(spex.getId()), Sort.by(Spexare_.LAST_NAME, Spexare_.FIRST_NAME, Spexare_.ID), Permission.READ)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .toList();
    }

    /**
     * @param id the spexare id
     * @return the spexare
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public SpexareDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(SPEXARE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    /**
     * @return the spexare of the caller's user, or empty when there is none
     */
    @RequiresAdminOrEditorOrUser
    public Optional<SpexareDto> findByCurrentUser() {
        final String externalId = getCurrentUserSubClaim();

        return hasText(externalId) ?
                repository
                        .findByUserExternalId(externalId)
                        .map(SPEXARE_MAPPER::toDto) :
                Optional.empty();
    }

    /**
     * @param id the spexare id
     * @return whether the spexare exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    /**
     * @param ids    the ids of the spexare, or empty to use the filter
     * @param filter a filter expression, or blank for all
     * @param sort   the order
     * @return the spexare the caller may read, streamed when iterated, so it must be consumed inside a transaction
     */
    @RequiresAdminOrEditorOrUser
    public Iterable<Spexare> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<Spexare> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<Spexare>builder()
                        .build(parseFilter(filter), SpexareSpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * Adds a spexare. The social security number is kept only when the caller may set it.
     *
     * @param dto the new spexare
     * @return the created spexare
     */
    @RequiresAdminOrEditorOrRevival
    public SpexareDto create(final SpexareCreateDto dto) {
        return Optional.of(SPEXARE_MAPPER.toModel(SpexareSensitiveData.isReadableForAll() ? dto : dto.toBuilder().socialSecurityNumber(null).build()))
                .map(repository::save)
                .map(SPEXARE_MAPPER::toDto)
                .orElseThrow(() -> new InternalErrorException("Could not create spexare"));
    }

    /**
     * @param dto the new values
     * @return the updated spexare
     */
    @RequiresAdminOrEditorOrUser
    public SpexareDto update(final SpexareUpdateDto dto) {
        return partialUpdate(dto);
    }

    /**
     * Changes the values given; null values are left unchanged, and so is the social security number when the caller
     * may not set it.
     *
     * @param dto the spexare id and new values
     * @return the updated spexare
     */
    @RequiresAdminOrEditorOrUser
    public SpexareDto partialUpdate(final SpexareUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(spexare -> {
                    SPEXARE_MAPPER.toPartialModel(SpexareSensitiveData.isReadable(spexare) ? dto : withoutSensitiveData(dto), spexare);
                    return spexare;
                })
                .map(repository::save)
                .map(SPEXARE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, dto.id()));
    }

    /**
     * Removes the spexare with everything that belongs to it and the permission grants on it. The audit history stays
     * unless it is purged with {@link nu.fgv.register.server.audit.AuditPurgeService}.
     *
     * @param id the spexare id
     */
    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long id) {
        if (doesSpexareExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(spexare -> {
                        permissionGrantService.deleteBySpexare(spexare);
                        repository.delete(spexare);
                    });
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    /**
     * @param id    the spexare id
     * @param image a PNG, JPEG, GIF or SVG image
     * @return the spexare
     */
    @RequiresAdminOrEditorOrUser
    public SpexareDto saveImage(final Long id, final byte[] image) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spexare -> {
                    spexare.setImage(Image.of(image, requireSupportedImageMimeType(image)));
                    repository.save(spexare);
                    return SPEXARE_MAPPER.toDto(spexare);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    /**
     * @param id the spexare id
     * @return the spexare
     */
    @RequiresAdminOrEditorOrUser
    public SpexareDto deleteImage(final Long id) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spexare -> {
                    spexare.setImage(null);
                    repository.save(spexare);
                    return SPEXARE_MAPPER.toDto(spexare);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    /**
     * @param id the spexare id
     * @return the image's bytes and content type
     * @throws nu.fgv.register.server.util.error.ResourceNoValueException when the spexare has no image
     */
    @RequiresAdminOrEditorOrUser
    public Pair<byte[], String> getImage(final Long id) {
        if (!doesSpexareExist(id)) {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
        return repository
                .findById0(id)
                .map(Spexare::getImage)
                .map(image -> Pair.of(image.getData(), image.getContentType()))
                .orElseThrow(() -> new ResourceNoValueException(Spexare.class, Spexare_.IMAGE, id));
    }

    /**
     * @param id the spexare id
     * @return the partner, or empty when there is none or the caller may not read it
     */
    @RequiresAdminOrEditorOrUser
    public Optional<SpexareDto> findPartnerBySpexare(final Long id) {
        if (doesSpexareExist(id)) {
            return repository
                    .findById0(id)
                    .filter(spexare -> spexare.getPartner() != null)
                    .map(Spexare::getPartner)
                    .filter(permissionService::hasReadPermission)
                    .map(SPEXARE_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    /**
     * For batch loading.
     *
     * @param ids spexare ids
     * @return the partners the caller may read, by spexare id
     */
    @RequiresAdminOrEditorOrUser
    public Map<Long, SpexareDto> findPartnersBySpexare(final Collection<Long> ids) {
        return repository.findWithPartnerByIdIn(ids).stream()
                .filter(spexare -> spexare.getPartner() != null && permissionService.hasReadPermission(spexare.getPartner()))
                .collect(Collectors.toMap(Spexare::getId, spexare -> SPEXARE_MAPPER.toDto(spexare.getPartner())));
    }

    /**
     * Makes the two spexare each other's partner, after parting either from a previous partner. The caller must be
     * allowed to change both.
     *
     * @param spexareId the spexare id
     * @param id        the partner's spexare id
     */
    @RequiresAdminOrEditorOrUser
    public void addPartner(final Long spexareId, final Long id) {
        if (doSpexareAndPartnerExist(spexareId, id)) {
            final Spexare spexare = repository.findById(spexareId)
                    .map(permissionService::checkWritePermission)
                    .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, spexareId));

            final Spexare partner = repository.findById(id)
                    .map(permissionService::checkWritePermission)
                    .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));

            if (spexare.getPartner() != null) {
                removePartner(spexare.getId());
            }
            if (partner.getPartner() != null) {
                removePartner(partner.getId());
            }

            spexare.setPartner(partner);
            partner.setPartner(spexare);

            repository.saveAndFlush(spexare);
            repository.saveAndFlush(partner);
        } else {
            throw new ResourcesNotFoundException(new String[]{Spexare.class.getSimpleName(), "Partner"}, spexareId, id);
        }
    }

    /**
     * Parts the spexare and its partner. The caller must be allowed to change both.
     *
     * @param id the spexare id
     */
    @RequiresAdminOrEditorOrUser
    public void removePartner(final Long id) {
        if (doesSpexareExist(id)) {
            repository
                    .findById0(id)
                    .map(permissionService::checkWritePermission)
                    .filter(spexare -> spexare.getPartner() != null)
                    .ifPresent(spexare -> {
                        final Spexare partner = spexare.getPartner();

                        permissionService.checkWritePermission(partner);

                        partner.setPartner(null);
                        spexare.setPartner(null);

                        repository.saveAndFlush(partner);
                        repository.saveAndFlush(spexare);
                    });
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    private static Deque<?> parseFilter(final String filter) {
        final Deque<?> parsed = FilterParser.parse(filter);

        if (!SpexareSensitiveData.isReadableForAll() && parsed.stream()
                .anyMatch(token -> token instanceof final FilterCriteria criteria && SpexareSensitiveData.isSensitivePath(criteria.key()))) {
            throw new BadRequestException("Filtering on %s is not permitted".formatted(SpexareSensitiveData.FIELD));
        }
        if (SpexareSensitiveData.isLimitedToRevival() && parsed.stream()
                .anyMatch(token -> token instanceof final FilterCriteria criteria && SpexareSensitiveData.isRestrictedPath(criteria.key()))) {
            throw new BadRequestException("Filtering on %s is not permitted".formatted(String.join(" or ", SpexareSensitiveData.RESTRICTED_FIELDS)));
        }

        return parsed;
    }

    private static void requirePermittedAggregationFilters(final List<AggregationFilter> aggregationFilters) {
        if (SpexareSensitiveData.isLimitedToRevival() && aggregationFilters.stream().anyMatch(SpexareService::isMembershipFilter)) {
            throw new BadRequestException("Filtering on %s is not permitted".formatted(FACET_MEMBERSHIPS));
        }
    }

    private static boolean isMembershipFilter(final AggregationFilter filter) {
        return FACET_MEMBERSHIPS.equals(filter.name())
                || (AGGREGATION_DATA_QUALITY.equals(filter.name()) && DataQualityIssue.fromKey(filter.value()).filter(DataQualityIssue.NO_MEMBERSHIP::equals).isPresent());
    }

    private static SpexareUpdateDto withoutSensitiveData(final SpexareUpdateDto dto) {
        return new SpexareUpdateDto(dto.id(), dto.firstName(), dto.lastName(), dto.nickName(), null,
                dto.deceased(), dto.published(), dto.graduation(), dto.comment());
    }

    private boolean doesSpexareExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

    private boolean doSpexareAndPartnerExist(final Long spexareId, final Long partnerId) {
        return doesSpexareExist(spexareId) && doesSpexareExist(partnerId);
    }

    private List<Facet> getFacets(final SearchResult<Spexare> searchResult) {
        final boolean readsUnpublished = isAdministratorOrEditor() || isRevival();
        final boolean readsMemberships = !SpexareSensitiveData.isLimitedToRevival();

        return AGGREGATIONS.stream()
                .filter(a -> {
                    if (Spexare_.PUBLISHED.equals(a) && !readsUnpublished) {
                        return false;
                    }
                    if (a.endsWith(AGGREGATION_COMPOSITE_DELIMITER + FACET_MEMBERSHIPS) && !readsMemberships) {
                        return false;
                    }
                    try {
                        searchResult.aggregation(AggregationKey.of(a));
                        return true;
                    } catch (final SearchException _) {
                        return false;
                    }
                })
                .map(a -> {
                    String baseFieldName = a;
                    String logicalKey = a;

                    if (a.contains(AGGREGATION_COMPOSITE_DELIMITER)) {
                        final String[] parts = a.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER));
                        baseFieldName = parts[0];
                        logicalKey = parts[1];
                    }

                    final Map<Object, Long> values = searchResult.aggregation(AggregationKey.of(a));
                    final String facetLabel = Objects.requireNonNullElse(messageSource.getMessage("facet." + logicalKey, null, logicalKey, LocaleContextHolder.getLocale()), logicalKey);

                    if (baseFieldName.contains(AGGREGATION_HIERARCHICAL_MARKER)) {
                        return Facet.builder()
                                .id(logicalKey)
                                .label(facetLabel)
                                .groups(createNestedGroups(logicalKey, values))
                                .build();
                    }

                    return Facet.builder()
                            .id(logicalKey)
                            .label(facetLabel)
                            .groups(List.of(FacetGroup.builder()
                                    .id(logicalKey)
                                    .label("")
                                    .values(createStandardValues(logicalKey, values))
                                    .build()))
                            .build();
                })
                .toList();
    }

    private List<FacetValue> createStandardValues(final String logicalKey, final Map<Object, Long> values) {
        return values.entrySet().stream()
                .map(entry -> {
                    final String rawValue = String.valueOf(entry.getKey());
                    String id = rawValue;
                    String displayValue = rawValue;

                    if (entry.getKey() instanceof final Boolean b) {
                        displayValue = messageSource.getMessage("boolean.%s".formatted(b), null, LocaleContextHolder.getLocale());
                    } else if (rawValue.contains(AGGREGATION_COMPOSITE_DELIMITER)) {
                        final String[] parts = rawValue.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER));
                        id = parts[0];
                        displayValue = parts[1];
                    } else if (FACET_COUNTRIES.equals(logicalKey)) {
                        displayValue = new Locale.Builder().setRegion(rawValue).build().getDisplayCountry(LocaleContextHolder.getLocale());
                    }
                    return new FacetValue(id, displayValue, entry.getValue());
                })
                .toList();
    }

    private List<FacetGroup> createNestedGroups(final String logicalKey, final Map<Object, Long> values) {
        final Map<String, List<FacetValue>> groups = new HashMap<>();
        final Map<String, String> groupLabelLookup = new HashMap<>();

        values.forEach((key, count) -> {
            final String rawValue = String.valueOf(key);
            if (rawValue.contains(AGGREGATION_COMPOSITE_DELIMITER)) {
                final String[] parts = rawValue.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER));
                final String technicalValueId = parts[0];

                if (parts.length > 2) {
                    final String groupId = technicalValueId.split(":", 2)[0];

                    groupLabelLookup.put(groupId, parts[1]);
                    groups.computeIfAbsent(groupId, _ -> new ArrayList<>())
                            .add(new FacetValue(technicalValueId, parts[2], count));
                } else {
                    groupLabelLookup.put(technicalValueId, "");
                    groups.computeIfAbsent(technicalValueId, _ -> new ArrayList<>())
                            .add(new FacetValue(technicalValueId, parts[1], count));
                }
            }
        });

        return groupLabelLookup.entrySet().stream()
                .map(entry -> FacetGroup.builder()
                        .id(entry.getKey())
                        .label(entry.getValue())
                        .values(groups.getOrDefault(entry.getKey(), List.of()))
                        .build())
                .toList();
    }

}
