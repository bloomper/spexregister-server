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

package nu.fgv.register.server.spexare.consent;

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
@WebMvcTest(value = ConsentApi.class)
class ConsentApiTest extends AbstractApiTest {

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the consent"),
            fieldWithPath("value").description("The value of the consent"),
            linksSubsection
    ).andWithPrefix("type.", Stream.of(typeResponseFieldDescriptors, auditResponseFieldsDescriptors).flatMap(Collection::stream).toList());
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("consents").description("Link to the current spexare's consents")
    );
    @MockitoBean
    private ConsentService service;

    @Test
    void should_get_paged() throws Exception {
        final var consent1 = ConsentDto.builder().id(1L).value(true).type(TypeDto.builder().id("PUBLISH").type(TypeType.CONSENT).build()).build();
        final var consent2 = ConsentDto.builder().id(2L).value(false).type(TypeDto.builder().id("CIRCULARS").type(TypeType.CONSENT).build()).build();

        when(service.findBySpexare(any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(consent1, consent2), PageRequest.of(1, 2, Sort.by("type")), 10));

        mockMvc
                .perform(
                        get("/api/spexare/{spexareId}/consents?page=1&size=2&sort=type,desc", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.consents", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-consent-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.consents[]").description("The elements"),
                                        fieldWithPath("_embedded.consents[].id").description("The id of the consent"),
                                        fieldWithPath("_embedded.consents[].value").description("The value of the consent"),
                                        fieldWithPath("_embedded.consents[].type").description("The type of the consent"),
                                        fieldWithPath("_embedded.consents[].createdBy").description("Who created the consent"),
                                        fieldWithPath("_embedded.consents[].createdAt").description("When was the consent created"),
                                        fieldWithPath("_embedded.consents[].lastModifiedBy").description("Who last modified the consent"),
                                        fieldWithPath("_embedded.consents[].lastModifiedAt").description("When was the consent last modified"),
                                        subsectionWithPath("_embedded.consents[]._links").description("The consent links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ConsentApi.class, "retrieve", Long.class, Pageable.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var consent = ConsentDto.builder().id(1L).value(true).type(TypeDto.builder().id("PUBLISH").type(TypeType.CONSENT).build()).build();

        when(service.findById(any(Long.class), any(Long.class))).thenReturn(consent);

        mockMvc
                .perform(
                        get("/api/spexare/{spexareId}/consents/{id}", 1L, 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-consent-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the consent")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ConsentApi.class, "retrieve", Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(ConsentCreateDto.class);
        final var dto = ConsentCreateDto.builder().value(Boolean.TRUE).build();
        final var consent = ConsentDto.builder().id(1L).value(true).type(TypeDto.builder().id("PUBLISH").type(TypeType.CONSENT).build()).build();

        when(service.create(any(Long.class), any(String.class), any(ConsentCreateDto.class))).thenReturn(consent);

        mockMvc
                .perform(
                        post("/api/spexare/{spexareId}/consents/{typeId}", 1L, consent.getId())
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare-consent-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the consent")
                                ),
                                requestFields(
                                        fields.withPath("value").description("The value of the consent")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(ConsentApi.class, "create", Long.class, String.class, ConsentCreateDto.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(ConsentUpdateDto.class);
        final var dto = ConsentUpdateDto.builder().id(1L).value(Boolean.TRUE).build();
        final var consent = ConsentDto.builder().id(1L).value(true).type(TypeDto.builder().id("PUBLISH").type(TypeType.CONSENT).build()).build();

        when(service.update(any(Long.class), any(String.class), any(Long.class), any(ConsentUpdateDto.class))).thenReturn(consent);

        mockMvc
                .perform(
                        put("/api/spexare/{spexareId}/consents/{typeId}/{id}", 1L, consent.getType().getId(), consent.getId())
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare-consent-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the consent"),
                                        parameterWithName("id").description("The id of the consent")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the consent"),
                                        fields.withPath("value").description("The value of the consent")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(ConsentApi.class, "update", Long.class, String.class, Long.class, ConsentUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        mockMvc
                .perform(
                        delete("/api/spexare/{spexareId}/consents/{typeId}/{id}", 1L, "PUBLISH", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare-consent-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the consent"),
                                        parameterWithName("id").description("The id of the consent")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(ConsentApi.class, "delete", Long.class, String.class, Long.class))
                        )
                );
    }

}
