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

package nu.fgv.register.server.spexare.toggle;

import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
@WebMvcTest(value = ToggleApi.class)
class ToggleApiTest extends AbstractApiTest {

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the toggle"),
            fieldWithPath("value").description("The value of the toggle"),
            linksSubsection
    ).andWithPrefix("type.", Stream.of(typeResponseFieldDescriptors, auditResponseFieldsDescriptors).flatMap(Collection::stream).toList());
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("toggles").description("Link to the current spexare's toggles")
    );
    @MockitoBean
    private ToggleService service;

    @Test
    void should_get_paged() throws Exception {
        final var toggle1 = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();
        final var toggle2 = ToggleDto.builder().id(2L).value(false).type(TypeDto.builder().id("CHALMERS_STUDENT").type(TypeType.TOGGLE).build()).build();

        when(service.findBySpexare(any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(toggle1, toggle2), PageRequest.of(1, 2, Sort.by("type")), 10));

        mockMvc
                .perform(
                        get("/api/spexare/{spexareId}/toggles?page=1&size=2&sort=type,desc", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.toggles", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-toggle-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.toggles[]").description("The elements"),
                                        fieldWithPath("_embedded.toggles[].id").description("The id of the toggle"),
                                        fieldWithPath("_embedded.toggles[].value").description("The value of the toggle"),
                                        fieldWithPath("_embedded.toggles[].type").description("The type of the toggle"),
                                        fieldWithPath("_embedded.toggles[].createdBy").description("Who created the toggle"),
                                        fieldWithPath("_embedded.toggles[].createdAt").description("When was the toggle created"),
                                        fieldWithPath("_embedded.toggles[].lastModifiedBy").description("Who last modified the toggle"),
                                        fieldWithPath("_embedded.toggles[].lastModifiedAt").description("When was the toggle last modified"),
                                        subsectionWithPath("_embedded.toggles[]._links").description("The toggle links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ToggleApi.class, "retrieve", Long.class, Pageable.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var toggle = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();

        when(service.findById(any(Long.class), any(Long.class))).thenReturn(toggle);

        mockMvc
                .perform(
                        get("/api/spexare/{spexareId}/toggles/{id}", 1L, 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-toggle-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the toggle")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ToggleApi.class, "retrieve", Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(ToggleCreateDto.class);
        final var dto = ToggleCreateDto.builder().value(Boolean.TRUE).build();
        final var toggle = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();

        when(service.create(any(Long.class), any(String.class), any(ToggleCreateDto.class))).thenReturn(toggle);

        mockMvc
                .perform(
                        post("/api/spexare/{spexareId}/toggles/{typeId}", 1L, toggle.getId())
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare-toggle-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the toggle")
                                ),
                                requestFields(
                                        fields.withPath("value").description("The value of the toggle")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(ToggleApi.class, "create", Long.class, String.class, ToggleCreateDto.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(ToggleUpdateDto.class);
        final var dto = ToggleUpdateDto.builder().id(1L).value(Boolean.TRUE).build();
        final var toggle = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();

        when(service.update(any(Long.class), any(String.class), any(Long.class), any(ToggleUpdateDto.class))).thenReturn(toggle);

        mockMvc
                .perform(
                        put("/api/spexare/{spexareId}/toggles/{typeId}/{id}", 1L, toggle.getType().getId(), toggle.getId())
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare-toggle-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the toggle"),
                                        parameterWithName("id").description("The id of the toggle")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the toggle"),
                                        fields.withPath("value").description("The value of the toggle")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ToggleApi.class, "update", Long.class, String.class, Long.class, ToggleUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        mockMvc
                .perform(
                        delete("/api/spexare/{spexareId}/toggles/{typeId}/{id}", 1L, "DECEASED", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare-toggle-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the toggle"),
                                        parameterWithName("id").description("The id of the toggle")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(ToggleApi.class, "delete", Long.class, String.class, Long.class))
                        )
                );
    }

}
