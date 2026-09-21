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

package nu.fgv.register.server.analytics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.analytics.AnalyticsDto.DataQualityDto;
import nu.fgv.register.server.analytics.AnalyticsDto.DemographicsDto;
import nu.fgv.register.server.analytics.AnalyticsDto.LifecycleDto;
import nu.fgv.register.server.analytics.AnalyticsDto.OperationsDto;
import nu.fgv.register.server.analytics.AnalyticsDto.ParticipationDto;
import nu.fgv.register.server.analytics.AnalyticsDto.TotalsDto;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.DataQualityIssue;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.FacetGroup;
import nu.fgv.register.server.util.search.FacetValue;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_ACTOR_VOCALS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_ADDRESS_TYPES;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_CONSENTS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_COUNTRIES;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_DEBUT_YEARS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_DECEASED;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_LAST_ACTIVE_YEARS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_MEMBERSHIPS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_SPEX_CATEGORY_NAMES;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_SPEX_COUNTS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_SPEX_TITLES;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_SPEX_YEARS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_TAGS;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_TASK_CATEGORY_NAMES;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_TASK_NAMES;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.FACET_TOGGLES;
import static nu.fgv.register.server.util.security.SecurityUtil.isAdministrator;
import static nu.fgv.register.server.util.security.SecurityUtil.isEditor;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TOP_N = 15;
    private static final int TOP_EDITORS = 10;
    private static final int REVISION_MONTHS = 24;
    private static final int ACTIVE_YEARS = 3;
    private static final int LAPSED_YEARS = 10;
    private static final String QUALITY_FACET = "quality";

    private final SpexareService spexareService;
    private final SpexareRepository spexareRepository;
    private final UserRepository userRepository;
    private final SpexRepository spexRepository;
    private final TaskRepository taskRepository;
    private final EntityManager entityManager;
    private final MessageSource messageSource;

    @RequiresAdminOrEditorOrUser
    public AnalyticsDto getAnalytics(final Set<AnalyticsSection> sections) {
        final boolean needsFacets = sections.stream().anyMatch(AnalyticsSection.FACET_BACKED::contains);
        final Map<String, Facet> facets = needsFacets ?
                spexareService.facets().stream().collect(toMap(Facet::getId, Function.identity(), (first, _) -> first)) :
                Map.of();
        final long total = needsFacets ? totalSpexare(facets) : 0L;
        final boolean isEditorOrAdmin = isEditor() || isAdministrator();

        return AnalyticsDto.builder()
                .totals(sections.contains(AnalyticsSection.TOTALS) ? totals() : null)
                .participation(sections.contains(AnalyticsSection.PARTICIPATION) ? participation(facets) : null)
                .demographics(sections.contains(AnalyticsSection.DEMOGRAPHICS) ? demographics(facets, total) : null)
                .lifecycle(sections.contains(AnalyticsSection.LIFECYCLE) ? lifecycle(facets, total) : null)
                .dataQuality(sections.contains(AnalyticsSection.DATA_QUALITY) && isEditorOrAdmin ? dataQuality(total) : null)
                .operations(sections.contains(AnalyticsSection.OPERATIONS) && isAdministrator() ? operations() : null)
                .build();
    }

    private TotalsDto totals() {
        return TotalsDto.builder()
                .spexareCount(spexareRepository.countByPublishedTrue())
                .spexareCountHistory(history(Spexare.class, (cb, root) -> cb.isTrue(root.get("published"))))
                .userCount(userRepository.count())
                .userCountHistory(history(User.class, null))
                .spexCount(spexRepository.countByParentIsNull())
                .spexCountHistory(history(Spex.class, (cb, root) -> cb.isNull(root.get("parent"))))
                .spexRevivalCount(spexRepository.countByParentIsNotNull())
                .spexRevivalCountHistory(history(Spex.class, (cb, root) -> cb.isNotNull(root.get("parent"))))
                .taskCount(taskRepository.count())
                .taskCountHistory(history(Task.class, null))
                .build();
    }

    private <T> List<HistoryDto> history(final Class<T> clazz, @Nullable final BiFunction<CriteriaBuilder, Root<T>, Predicate> filter) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        final Root<T> root = criteriaQuery.from(clazz);

        final Expression<Integer> year = criteriaBuilder.function("year", Integer.class, root.get("createdAt"));
        final Expression<Integer> month = criteriaBuilder.function("month", Integer.class, root.get("createdAt"));

        criteriaQuery.select(criteriaBuilder.tuple(year, month, criteriaBuilder.count(root)));

        final Instant threeYearsAgo = Instant.now().minus(3L * 365, ChronoUnit.DAYS);
        final Predicate historyPredicate = criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), threeYearsAgo);

        criteriaQuery.where(filter == null ?
                historyPredicate :
                criteriaBuilder.and(historyPredicate, filter.apply(criteriaBuilder, root)));
        criteriaQuery.groupBy(year, month);
        criteriaQuery.orderBy(criteriaBuilder.asc(year), criteriaBuilder.asc(month));

        return entityManager.createQuery(criteriaQuery)
                .getResultList()
                .stream()
                .map(tuple -> new HistoryDto(
                        "%s-%02d".formatted(tuple.get(0), (Integer) tuple.get(1)),
                        (Long) tuple.get(2)
                ))
                .toList();
    }

    private ParticipationDto participation(final Map<String, Facet> facets) {
        return ParticipationDto.builder()
                .bySpexYear(byKey(facets, FACET_SPEX_YEARS))
                .bySpexCategory(byCount(facets, FACET_SPEX_CATEGORY_NAMES, TOP_N))
                .topSpex(byCount(facets, FACET_SPEX_TITLES, TOP_N))
                .byTaskCategory(byCount(facets, FACET_TASK_CATEGORY_NAMES, TOP_N))
                .topTask(byCount(facets, FACET_TASK_NAMES, TOP_N))
                .byVocal(byCount(facets, FACET_ACTOR_VOCALS, TOP_N))
                .build();
    }

    private DemographicsDto demographics(final Map<String, Facet> facets, final long total) {
        return DemographicsDto.builder()
                .byCountry(byCount(facets, FACET_COUNTRIES, TOP_N))
                .byAddressType(byCount(facets, FACET_ADDRESS_TYPES, TOP_N))
                .byMembership(byCount(facets, FACET_MEMBERSHIPS, TOP_N))
                .byTag(byCount(facets, FACET_TAGS, TOP_N))
                .byToggle(byCount(facets, FACET_TOGGLES, TOP_N))
                .byStatus(status(facets))
                .consentCompletion(consentCompletion(facets, total))
                .build();
    }

    private List<BucketDto> status(final Map<String, Facet> facets) {
        return values(facets, FACET_DECEASED).stream()
                .map(value -> BucketDto.of(
                        value.getId(),
                        message("analytics.status." + ("true".equalsIgnoreCase(value.getId()) ? "deceased" : "living"), value.getLabel()),
                        value.getCount(),
                        FACET_DECEASED))
                .toList();
    }

    private List<ConsentCompletionDto> consentCompletion(final Map<String, Facet> facets, final long total) {
        final Facet facet = facets.get(FACET_CONSENTS);

        if (facet == null || facet.getGroups() == null) {
            return List.of();
        }

        return facet.getGroups().stream()
                .map(group -> {
                    final long granted = countEndingWith(group, ":true");
                    final long denied = countEndingWith(group, ":false");

                    return ConsentCompletionDto.builder()
                            .key(group.getId())
                            .label(group.getLabel())
                            .granted(granted)
                            .denied(denied)
                            .missing(Math.max(0L, total - granted - denied))
                            .build();
                })
                .sorted(Comparator.comparing(ConsentCompletionDto::label))
                .toList();
    }

    private LifecycleDto lifecycle(final Map<String, Facet> facets, final long total) {
        final List<BucketDto> lastActive = byKey(facets, FACET_LAST_ACTIVE_YEARS);
        final List<BucketDto> engagement = engagement(facets);
        final long everActive = lastActive.stream().mapToLong(BucketDto::count).sum();

        return LifecycleDto.builder()
                .newcomersByYear(byKey(facets, FACET_DEBUT_YEARS))
                .lastActiveByYear(lastActive)
                .byEngagement(engagement)
                .byDormancy(dormancy(lastActive, total, everActive))
                .oneTimers(sumEngagement(engagement, 1, 1))
                .returning(sumEngagement(engagement, 2, 4))
                .veterans(sumEngagement(engagement, 5, Integer.MAX_VALUE))
                .neverActive(Math.max(0L, total - everActive))
                .build();
    }

    private List<BucketDto> dormancy(final List<BucketDto> lastActiveByYear, final long total, final long everActive) {
        final int currentYear = LocalDate.now().getYear();
        final int activeFrom = currentYear - ACTIVE_YEARS + 1;
        final int lapsedFrom = currentYear - LAPSED_YEARS + 1;

        return List.of(
                band("active", activeFrom, null, lastActiveByYear),
                band("lapsing", lapsedFrom, activeFrom - 1, lastActiveByYear),
                band("dormant", null, lapsedFrom - 1, lastActiveByYear),
                BucketDto.of(DataQualityIssue.NO_ACTIVITY.getKey(), message("analytics.dormancy.never", "never"),
                        Math.max(0L, total - everActive), QUALITY_FACET)
        );
    }

    private BucketDto band(final String name, final @Nullable Integer from, final @Nullable Integer to, final List<BucketDto> lastActiveByYear) {
        final long count = lastActiveByYear.stream()
                .filter(bucket -> isYearWithin(bucket.key(), from, to))
                .mapToLong(BucketDto::count)
                .sum();

        return BucketDto.of(
                (from == null ? "" : String.valueOf(from)) + ".." + (to == null ? "" : String.valueOf(to)),
                message("analytics.dormancy." + name, name),
                count,
                FACET_LAST_ACTIVE_YEARS);
    }

    private boolean isYearWithin(final String key, final @Nullable Integer from, final @Nullable Integer to) {
        try {
            final int year = Integer.parseInt(key);

            return (from == null || year >= from) && (to == null || year <= to);
        } catch (final NumberFormatException _) {
            return false;
        }
    }

    private List<BucketDto> engagement(final Map<String, Facet> facets) {
        return values(facets, FACET_SPEX_COUNTS).stream()
                .filter(value -> !"0".equals(value.getId()))
                .map(value -> BucketDto.of(value.getId(), value.getLabel(), value.getCount(), FACET_SPEX_COUNTS))
                .sorted(Comparator.comparingInt(bucket -> Integer.parseInt(bucket.key())))
                .toList();
    }

    private long sumEngagement(final List<BucketDto> engagement, final int from, final int to) {
        return engagement.stream()
                .filter(bucket -> {
                    final int count = Integer.parseInt(bucket.key());

                    return count >= from && count <= to;
                })
                .mapToLong(BucketDto::count)
                .sum();
    }

    private DataQualityDto dataQuality(final long total) {
        final List<BucketDto> issues = new ArrayList<>();

        for (final DataQualityIssue issue : DataQualityIssue.values()) {
            issues.add(BucketDto.of(
                    issue.getKey(),
                    message("analytics.dataQuality." + issue.getKey(), issue.getKey()),
                    spexareService.countByDataQualityIssue(issue),
                    QUALITY_FACET));
        }
        issues.sort(Comparator.comparingLong(BucketDto::count).reversed());

        return DataQualityDto.builder()
                .total(total)
                .issues(issues)
                // The worst single gap bounds how many records can be free of all of them.
                .complete(Math.max(0L, total - issues.stream().mapToLong(BucketDto::count).max().orElse(0L)))
                .build();
    }

    private OperationsDto operations() {
        final long since = Instant.now().minus(REVISION_MONTHS * 30L, ChronoUnit.DAYS).toEpochMilli();

        return OperationsDto.builder()
                .usersByState(usersByState())
                .usersWithoutSpexare(count("SELECT COUNT(u) FROM User u WHERE u.spexare IS NULL"))
                .spexareWithoutUser(count("SELECT COUNT(s) FROM Spexare s WHERE s.user IS NULL"))
                .revisionsByMonth(revisionsByMonth(since))
                .revisionsBySource(revisionsBySource(since))
                .topEditors(topEditors(since))
                .build();
    }

    private List<BucketDto> usersByState() {
        return entityManager
                .createQuery("SELECT u.state.id, u.state.labels, COUNT(u) FROM User u GROUP BY u.state.id, u.state.labels", Object[].class)
                .getResultList()
                .stream()
                .map(row -> {
                    final String id = String.valueOf(row[0]);
                    @SuppressWarnings("unchecked") final Map<String, String> labels = (Map<String, String>) row[1];

                    return BucketDto.of(id, labels == null ? id : labels.getOrDefault(language(), id), (Long) row[2]);
                })
                .sorted(Comparator.comparingLong(BucketDto::count).reversed())
                .toList();
    }

    private List<BucketDto> revisionsByMonth(final long since) {
        final Map<String, Long> byMonth = entityManager
                .createQuery("SELECT r.modifiedAt FROM AuditRevisionEntity r WHERE r.modifiedAt >= :since", Long.class)
                .setParameter("since", since)
                .getResultList()
                .stream()
                .collect(Collectors.groupingBy(
                        millis -> {
                            final LocalDate date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate();

                            return "%d-%02d".formatted(date.getYear(), date.getMonthValue());
                        },
                        LinkedHashMap::new,
                        Collectors.counting()));

        return byMonth.entrySet().stream()
                .map(entry -> BucketDto.of(entry.getKey(), entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(BucketDto::key))
                .toList();
    }

    private List<BucketDto> revisionsBySource(final long since) {
        return entityManager
                .createQuery("SELECT r.source, COUNT(r) FROM AuditRevisionEntity r WHERE r.modifiedAt >= :since GROUP BY r.source", Object[].class)
                .setParameter("since", since)
                .getResultList()
                .stream()
                .map(row -> {
                    final String source = row[0] == null ? "UNKNOWN" : String.valueOf(row[0]);

                    return BucketDto.of(source, message("analytics.source." + source, source), (Long) row[1]);
                })
                .sorted(Comparator.comparingLong(BucketDto::count).reversed())
                .toList();
    }

    private List<BucketDto> topEditors(final long since) {
        return entityManager
                .createQuery("SELECT r.modifiedBy, COUNT(r) FROM AuditRevisionEntity r WHERE r.modifiedAt >= :since AND r.modifiedBy IS NOT NULL GROUP BY r.modifiedBy ORDER BY COUNT(r) DESC", Object[].class)
                .setParameter("since", since)
                .setMaxResults(TOP_EDITORS)
                .getResultList()
                .stream()
                .map(row -> BucketDto.of(String.valueOf(row[0]), String.valueOf(row[0]), (Long) row[1]))
                .toList();
    }

    private long count(final String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private long totalSpexare(final Map<String, Facet> facets) {
        return values(facets, FACET_DECEASED).stream()
                .mapToLong(FacetValue::getCount)
                .sum();
    }

    private List<BucketDto> byCount(final Map<String, Facet> facets, final String facetId, final int limit) {
        return buckets(facets, facetId)
                .sorted(Comparator.comparingLong(BucketDto::count).reversed().thenComparing(BucketDto::label))
                .limit(limit)
                .toList();
    }

    private List<BucketDto> byKey(final Map<String, Facet> facets, final String facetId) {
        return buckets(facets, facetId)
                .sorted(Comparator.comparing(BucketDto::key))
                .toList();
    }

    private Stream<BucketDto> buckets(final Map<String, Facet> facets, final String facetId) {
        final Facet facet = facets.get(facetId);

        if (facet == null || facet.getGroups() == null) {
            return Stream.empty();
        }

        return facet.getGroups().stream()
                .flatMap(group -> group.getValues() == null ?
                        Stream.empty() :
                        group.getValues().stream()
                                .map(value -> BucketDto.of(value.getId(), label(group, value), value.getCount(), facetId)));
    }

    private String label(final FacetGroup group, final FacetValue value) {
        return group.getLabel() == null || group.getLabel().isBlank() ?
                value.getLabel() :
                "%s: %s".formatted(group.getLabel(), value.getLabel());
    }

    private List<FacetValue> values(final Map<String, Facet> facets, final String facetId) {
        final Facet facet = facets.get(facetId);

        if (facet == null || facet.getGroups() == null) {
            return List.of();
        }

        return facet.getGroups().stream()
                .flatMap(group -> group.getValues() == null ? Stream.empty() : group.getValues().stream())
                .toList();
    }

    private long countEndingWith(final FacetGroup group, final String suffix) {
        return group.getValues() == null ?
                0L :
                group.getValues().stream()
                        .filter(value -> value.getId() != null && value.getId().endsWith(suffix))
                        .mapToLong(FacetValue::getCount)
                        .sum();
    }

    private String message(final String key, final String fallback) {
        return messageSource.getMessage(key, null, fallback, LocaleContextHolder.getLocale());
    }

    private String language() {
        return LocaleContextHolder.getLocale().getLanguage();
    }
}
