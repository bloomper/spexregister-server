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

import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = SavedSearchApi.class)
class SavedSearchApiTest extends AbstractApiTest {

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the saved search"),
            fieldWithPath("name").description("The name of the saved search"),
            fieldWithPath("query").description("The encoded search query of the saved search"),
            linksSubsection
    );
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("savedSearches").description("Link to the current user's saved searches").optional()
    );

    @MockitoBean
    private SavedSearchService service;

    @Test
    void should_get_all() throws Exception {
        final var savedSearch = SavedSearchDto.builder().id(1L).name("Inactive members").query("q=inactive").build();

        when(service.findAll()).thenReturn(List.of(savedSearch));

        mockMvc
                .perform(
                        get("/api/saved-searches")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(
                        document(
                                "saved-search-get-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SavedSearchApi.class, "retrieve"))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(SavedSearchCreateDto.class);
        final var savedSearch = SavedSearchDto.builder().id(1L).name("Inactive members").query("q=inactive").build();
        final var dto = SavedSearchCreateDto.builder().name("Inactive members").query("q=inactive").build();

        when(service.create(any(SavedSearchCreateDto.class))).thenReturn(savedSearch);

        mockMvc
                .perform(
                        post("/api/saved-searches")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "saved-search-create",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the saved search"),
                                        fields.withPath("query").description("The encoded search query of the saved search")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(SavedSearchApi.class, "create", SavedSearchCreateDto.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var savedSearch = SavedSearchDto.builder().id(1L).name("Inactive members").query("q=inactive").build();

        when(service.findById(anyLong())).thenReturn(savedSearch);

        mockMvc
                .perform(
                        get("/api/saved-searches/{id}", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "saved-search-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the saved search")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SavedSearchApi.class, "retrieve", Long.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(SavedSearchUpdateDto.class);
        final var savedSearch = SavedSearchDto.builder().id(1L).name("Inactive members").query("q=inactive").build();
        final var dto = SavedSearchUpdateDto.builder().id(1L).name("Inactive members").query("q=inactive").build();

        when(service.update(any(SavedSearchUpdateDto.class))).thenReturn(savedSearch);

        mockMvc
                .perform(
                        put("/api/saved-searches/{id}", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "saved-search-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the saved search")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the saved search"),
                                        fields.withPath("name").description("The name of the saved search"),
                                        fields.withPath("query").description("The encoded search query of the saved search")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SavedSearchApi.class, "update", Long.class, SavedSearchUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        doNothing().when(service).deleteById(anyLong());

        mockMvc
                .perform(
                        delete("/api/saved-searches/{id}", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "saved-search-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the saved search")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SavedSearchApi.class, "delete", Long.class))
                        )
                );
    }

}
