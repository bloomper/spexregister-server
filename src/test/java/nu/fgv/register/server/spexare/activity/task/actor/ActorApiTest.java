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

package nu.fgv.register.server.spexare.activity.task.actor;

import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = ActorApi.class)
class ActorApiTest extends AbstractApiTest {

    @MockBean
    private ActorService service;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the actor"),
            fieldWithPath("role").description("The role of the actor"),
            linksSubsection
    ).andWithPrefix("vocal.", Stream.of(typeResponseFieldDescriptors, auditResponseFieldsDescriptors).flatMap(Collection::stream).toList());

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("activities").description("Link to the current spexare's activities"),
            linkWithRel("task-activities").description("Link to the current spexare's task activities"),
            linkWithRel("actors").description("Link to the current spexare's actors")
    );

    @Test
    void should_get_paged() throws Exception {
        final var actor1 = ActorDto.builder().id(1L).build();
        final var actor2 = ActorDto.builder().id(2L).build();

        when(service.findByTaskActivity(any(Long.class), any(Long.class), any(Long.class), any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(actor1, actor2), PageRequest.of(1, 2, Sort.by("id")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors?page=1&size=2&sort=id,desc&filter=role:whatever", 1L, 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.actors", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-activity-task-actor-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskActivityId").description("The id of the task activity")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.actors[]").description("The elements"),
                                        fieldWithPath("_embedded.actors[].id").description("The id of the actor"),
                                        fieldWithPath("_embedded.actors[].role").description("The role of the actor"),
                                        fieldWithPath("_embedded.actors[].createdBy").description("Who created the actor"),
                                        fieldWithPath("_embedded.actors[].createdAt").description("When was the actor created"),
                                        fieldWithPath("_embedded.actors[].lastModifiedBy").description("Who last modified the actor"),
                                        fieldWithPath("_embedded.actors[].lastModifiedAt").description("When was the actor last modified"),
                                        subsectionWithPath("_embedded.actors[]._links").description("The actor links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ActorApi.class, "retrieve", Long.class, Long.class, Long.class, Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var actor = ActorDto.builder().id(1L).role("Alfred Nobel").vocal(TypeDto.builder().id("B1").type(TypeType.VOCAL).build()).build();

        when(service.findById(any(Long.class), any(Long.class), any(Long.class), any(Long.class))).thenReturn(actor);

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors/{id}", 1L, 1L, 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-activity-task-actor-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskActivityId").description("The id of the task activity"),
                                        parameterWithName("id").description("The id of the actor")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ActorApi.class, "retrieve", Long.class, Long.class, Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(ActorCreateDto.class);
        final var dto = ActorCreateDto.builder().role("Alfred Nobel").build();
        final var actor = ActorDto.builder().id(1L).role(dto.getRole()).vocal(TypeDto.builder().id("B1").type(TypeType.VOCAL).build()).build();

        when(service.create(any(Long.class), any(Long.class), any(Long.class), any(String.class), any(ActorCreateDto.class))).thenReturn(actor);

        mockMvc
                .perform(
                        post("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors/{vocalId}", 1L, 1L, 1L, "B1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare-activity-task-actor-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskActivityId").description("The id of the task activity"),
                                        parameterWithName("vocalId").description("The vocal id of the actor")
                                ),
                                requestFields(
                                        fields.withPath("role").description("The role of the actor")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(ActorApi.class, "create", Long.class, Long.class, Long.class, String.class, ActorCreateDto.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(ActorUpdateDto.class);
        final var actor = ActorDto.builder().id(1L).role("Alfred Nobel").vocal(TypeDto.builder().id("B1").type(TypeType.VOCAL).build()).build();
        final var dto = ActorUpdateDto.builder().id(1L).role("Alfred Nobel").build();

        when(service.update(any(Long.class), any(Long.class), any(Long.class), any(String.class), any(Long.class), any(ActorUpdateDto.class))).thenReturn(actor);

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors/{vocalId}/{id}", 1L, 1L, 1L, "B1", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andDo(document(
                                "spexare-activity-task-actor-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskActivityId").description("The id of the task activity"),
                                        parameterWithName("vocalId").description("The vocal id of the actor"),
                                        parameterWithName("id").description("The id of the task activity")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the actor"),
                                        fields.withPath("role").description("The role of the actor")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ActorApi.class, "update", Long.class, Long.class, Long.class, String.class, Long.class, ActorUpdateDto.class))
                        )
                );
    }

    @Test
    void should_partial_update() throws Exception {
        final var fields = new ConstrainedFields(ActorUpdateDto.class);
        final var actor = ActorDto.builder().id(1L).role("Alfred Nobel").vocal(TypeDto.builder().id("B1").type(TypeType.VOCAL).build()).build();
        final var dto = ActorUpdateDto.builder().id(1L).role("Alfred Nobel").build();

        when(service.partialUpdate(any(Long.class), any(Long.class), any(Long.class), any(String.class), any(Long.class), any(ActorUpdateDto.class))).thenReturn(actor);

        mockMvc
                .perform(
                        patch("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors/{vocalId}/{id}", 1L, 1L, 1L, "B1", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andDo(document(
                                "spexare-activity-task-actor-update-partial",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskActivityId").description("The id of the task activity"),
                                        parameterWithName("vocalId").description("The vocal id of the actor"),
                                        parameterWithName("id").description("The id of the task activity")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the actor"),
                                        fields.withPath("role").description("The role of the actor")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ActorApi.class, "partialUpdate", Long.class, Long.class, Long.class, String.class, Long.class, ActorUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors/{vocalId}/{id}", 1L, 1L, 1L, "B1", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare-activity-task-actor-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskActivityId").description("The id of the task activity"),
                                        parameterWithName("vocalId").description("The vocal id of the actor"),
                                        parameterWithName("id").description("The id of the task activity")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(ActorApi.class, "delete", Long.class, Long.class, Long.class, String.class, Long.class))
                        )
                );
    }

}
