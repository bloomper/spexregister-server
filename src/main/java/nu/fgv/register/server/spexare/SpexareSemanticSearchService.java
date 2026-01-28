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

package nu.fgv.register.server.spexare;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.util.search.AggregationFilter;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.PageWithFacets;
import nu.fgv.register.server.util.search.PageWithFacetsImpl;
import nu.fgv.register.server.util.search.WindowWithFacets;
import nu.fgv.register.server.util.search.WindowWithFacetsImpl;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Service
public class SpexareSemanticSearchService {

    private final SpexareService spexareService;
    private final SpexareRepository spexareRepository;
    private final SpexareFacetService facetService;
    private final VectorStore vectorStore;

    public WindowWithFacets<SpexareDto> search(final String query,
                                               final List<AggregationFilter> aggregationFilters,
                                               final int offset,
                                               final int limit,
                                               final Sort sort) {
        final List<String> list = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                //.topK(4)
                //.filterExpression() TODO
                .build())
                .stream()
                .map(Document::getText)
                .toList();


        final SemanticSearchExecution exec = executeSemanticSearch(query, aggregationFilters, offset, limit, sort);

        if (exec.isEmpty()) {
            return new WindowWithFacetsImpl<>(List.of(), _ -> ScrollPosition.offset(offset), false, List.of());
        }

        final boolean hasNext = exec.totalHitCount() > (long) offset + (long) limit;

        return new WindowWithFacetsImpl<>(
                exec.content(),
                _ -> ScrollPosition.offset(offset),
                hasNext,
                exec.facets()
        );
    }

    public PageWithFacets<SpexareDto> search(final String query,
                                             final List<AggregationFilter> aggregationFilters,
                                             final Pageable pageable) {
        final int offset = Math.toIntExact(pageable.getOffset());
        final int limit = pageable.getPageSize();

        final SemanticSearchExecution exec = executeSemanticSearch(query, aggregationFilters, offset, limit, pageable.getSort());

        if (exec.isEmpty()) {
            return new PageWithFacetsImpl<>(List.of(), pageable, SimpleSearchResultTotal.of(0, true), List.of());
        }

        return new PageWithFacetsImpl<>(
                exec.content(),
                pageable,
                exec.total(),
                exec.facets()
        );
    }

    private SemanticSearchExecution executeSemanticSearch(final String query,
                                                          final List<AggregationFilter> aggregationFilters,
                                                          final int offset,
                                                          final int limit,
                                                          final Sort sort) {
        /*
        if (!hasText(query)) {
            return SemanticSearchExecution.empty();
        }

        final float[] queryVector = embeddingService.embed(query);
        if (queryVector.length == 0) {
            return SemanticSearchExecution.empty();
        }

        final boolean publishedOnly = !isAdministrator();
        final int semanticCandidateLimitForFacets = props.embedding().semanticCandidateLimitForFacets();
        final List<Float> vector = toFloatList(queryVector);

        final List<Long> semanticCandidateIds = qdrantClient.search(
                        vector,
                        0,
                        semanticCandidateLimitForFacets,
                        publishedOnly
                )
                .stream()
                .mapToLong(QdrantClient.ScoredPoint::id)
                .boxed()
                .toList();

        if (semanticCandidateIds.isEmpty()) {
            return SemanticSearchExecution.empty();
        }

        final List<Long> allowedCandidateIds = spexareService.getAllowedIdsByIds(semanticCandidateIds);

        if (allowedCandidateIds.isEmpty()) {
            return SemanticSearchExecution.empty();
        }

        final SearchResult<Spexare> facetSearchResult = spexareRepository.search(
                "",
                aggregationFilters,
                allowedCandidateIds,
                0,
                0,
                Sort.by("score")
        );

        final List<Facet> facets = facetService.getFacets(facetSearchResult);
        final SearchResultTotal total = facetSearchResult.total();
        final long totalHitCount = total.hitCount();

        final List<QdrantClient.ScoredPoint> pageHits = qdrantClient.search(
                vector,
                offset,
                limit,
                publishedOnly
        );

        final List<SpexareDto> content = mapHitsToDtos(pageHits);

        return new SemanticSearchExecution(content, facets, total, totalHitCount);

         */
        return null;
    }

    /*
    private List<SpexareDto> mapHitsToDtos(final List<QdrantClient.ScoredPoint> pageHits) {
        final List<Long> pageIds = pageHits.stream()
                .mapToLong(QdrantClient.ScoredPoint::id)
                .boxed()
                .toList();

        if (pageIds.isEmpty()) {
            return List.of();
        }

        final List<Spexare> allowedPage = spexareService.getAllowedByIds(pageIds);
        final Map<Long, Spexare> spexareById = new LinkedHashMap<>();

        for (final Spexare s : allowedPage) {
            spexareById.put(s.getId(), s);
        }

        return pageHits.stream()
                .map(h -> spexareById.get(h.id()))
                .filter(Objects::nonNull)
                .map(SPEXARE_MAPPER::toDto)
                .toList();
    }

*/
    private record SemanticSearchExecution(
            List<SpexareDto> content,
            List<Facet> facets,
            @Nullable SearchResultTotal total,
            long totalHitCount
    ) {
        static SemanticSearchExecution empty() {
            return new SemanticSearchExecution(List.of(), List.of(), null, 0L);
        }

        boolean isEmpty() {
            return total == null;
        }
    }
}
