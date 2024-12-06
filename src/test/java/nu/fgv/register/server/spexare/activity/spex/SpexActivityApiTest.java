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

package nu.fgv.register.server.spexare.activity.spex;

import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = SpexActivityApi.class)
class SpexActivityApiTest extends AbstractApiTest {

    @MockitoBean
    private SpexActivityService service;

    @MockitoBean
    private SpexApi spexApi;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex activity"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("activities").description("Link to the current spexare's activities"),
            linkWithRel("spex-activity").description("Link to the current spexare's spex activity"),
            linkWithRel("spex").description("Link to the current spex")
    );

    private final ResponseFieldsSnippet spexResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex"),
            fieldWithPath("year").description("The year of the spex"),
            fieldWithPath("title").description("The title of the spex"),
            fieldWithPath("revival").description("The revival flag of the spex"),
            fieldWithPath("posterUrl").description("The poster URL of the spex"),
            linksSubsection
    );

    private final LinksSnippet spexLinks = baseLinks.and(
            linkWithRel("poster").description("Link to the current spex's poster").optional(),
            linkWithRel("parent").description("Link to the current spex's parent").optional(),
            linkWithRel("revivals").description("Link to the current spex's revivals").optional(),
            linkWithRel("category").description("Link to the current spex's spex category").optional(),
            linkWithRel("spex").description("Link to paged spex").optional(),
            linkWithRel("spex-including-revivals").description("Link to paged spex (including revivals)").optional()
    );

    @Test
    void should_get() throws Exception {
        final var spexActivity = SpexActivityDto.builder().id(1L).build();

        when(service.findByActivity(any(Long.class), any(Long.class))).thenReturn(spexActivity);

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activity", 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-activity-spex-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexActivityApi.class, "retrieve", Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_get_by_id() throws Exception {
        final var spexActivity = SpexActivityDto.builder().id(1L).build();

        when(service.findById(any(Long.class), any(Long.class), any(Long.class))).thenReturn(spexActivity);

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activity/{id}", 1L, 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-activity-spex-get-2",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the spex activity")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexActivityApi.class, "retrieve", Long.class, Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var spexActivity = SpexActivityDto.builder().id(1L).build();

        when(service.create(any(Long.class), any(Long.class), any(Long.class))).thenReturn(spexActivity);

        mockMvc
                .perform(
                        post("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activity/{spexId}", 1L, 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare-activity-spex-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("spexId").description("The id of the spex")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(SpexActivityApi.class, "create", Long.class, Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var spexActivity = SpexActivityDto.builder().id(1L).build();

        when(service.update(any(Long.class), any(Long.class), any(Long.class), any(Long.class))).thenReturn(spexActivity);

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activity/{id}/{spexId}", 1L, 1L, 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andDo(document(
                                "spexare-activity-spex-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("spexId").description("The id of the spex"),
                                        parameterWithName("id").description("The id of the spex activity")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexActivityApi.class, "update", Long.class, Long.class, Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activity/{id}", 1L, 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare-activity-spex-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the spex activity")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SpexActivityApi.class, "delete", Long.class, Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_get_spex() throws Exception {
        final var spex = SpexDto.builder().id(1L).year("2021").build();
        final var realSpexApi = new SpexApi(null, null, null, null, null, null);

        when(service.findSpexBySpexActivity(any(Long.class), any(Long.class), any(Long.class))).thenReturn(spex);
        when(spexApi.getLinks(any(SpexDto.class), eq(false))).thenReturn(realSpexApi.getLinks(spex, false));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activity/{id}/spex", 1L, 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-activity-spex-get-spex",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the spex activity")
                                ),
                                spexResponseFields,
                                spexLinks,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexActivityApi.class, "retrieveSpex", Long.class, Long.class, Long.class))
                        )
                );
    }

}
