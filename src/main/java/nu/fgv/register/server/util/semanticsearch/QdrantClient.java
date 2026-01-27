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

package nu.fgv.register.server.util.semanticsearch;

import nu.fgv.register.server.config.SemanticSearchProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class QdrantClient {

    private final RestClient restClient;
    private final SemanticSearchProperties props;

    public QdrantClient(final SemanticSearchProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.qdrant().baseUrl())
                .build();
    }

    public void ensureCollectionExists(final int vectorSize) {
        final String collection = props.qdrant().collection();

        try {
            restClient.get()
                    .uri("/collections/{name}", collection)
                    .retrieve()
                    .toBodilessEntity();
            return;
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }

        final var createBody = Map.of(
                "vectors", Map.of(
                        "size", vectorSize,
                        "distance", props.qdrant().distance()
                )
        );

        restClient.put()
                .uri("/collections/{name}", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createBody)
                .retrieve()
                .toBodilessEntity();
    }

    public void upsertPoints(final List<Point> points) {
        final String collection = props.qdrant().collection();
        final boolean wait = props.qdrant().doWait();
        final var body = Map.of("points", points);

        restClient.put()
                .uri("/collections/{name}/points?wait={wait}", collection, wait)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public List<ScoredPoint> search(final List<Float> queryVector,
                                    final int offset,
                                    final int limit,
                                    final boolean publishedOnly) {
        final String collection = props.qdrant().collection();

        final var request = Map.of(
                "vector", queryVector,
                "limit", limit,
                "offset", offset,
                "filter", publishedFilter(publishedOnly),
                "with_payload", false,
                "with_vector", false
        );

        final SearchResponse response = restClient.post()
                .uri("/collections/{name}/points/search", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SearchResponse.class);

        if (response == null || response.result() == null) {
            return List.of();
        }

        return response.result();
    }

    public long count(final boolean publishedOnly) {
        final String collection = props.qdrant().collection();

        final var request = Map.of(
                "filter", publishedFilter(publishedOnly),
                "exact", true
        );

        final CountResponse response = restClient.post()
                .uri("/collections/{name}/points/count", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CountResponse.class);

        if (response == null || response.result() == null) {
            return 0L;
        }

        return response.result().count();
    }

    private Map<String, Object> publishedFilter(final boolean publishedOnly) {
        return Map.of(
                "must", List.of(
                        Map.of(
                                "key", "published",
                                "match", Map.of("value", publishedOnly)
                        )
                )
        );
    }

    public record Point(
            long id,
            List<Float> vector,
            Map<String, Object> payload
    ) {
    }

    public record SearchResponse(
            List<ScoredPoint> result
    ) {
    }

    public record ScoredPoint(
            long id,
            double score
    ) {
    }

    public record CountResponse(
            CountResult result
    ) {
    }

    public record CountResult(
            long count
    ) {
    }
}
