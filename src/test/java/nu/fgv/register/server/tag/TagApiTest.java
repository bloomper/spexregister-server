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

package nu.fgv.register.server.tag;

import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
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

import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = TagApi.class)
class TagApiTest extends AbstractApiTest {

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the tag"),
            fieldWithPath("name").description("The name of the tag"),
            linksSubsection
    );
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("tags").description("Link to paged tags").optional(),
            linkWithRel("revisions").description("Link to revisions").optional()
    );
    @MockitoBean
    private TagService service;
    @MockitoBean
    private TagImportService importService;
    @MockitoBean
    private JobService jobService;

    @Test
    void should_get_paged() throws Exception {
        final var tag1 = TagDto.builder().id(1L).name("tag1").build();
        final var tag2 = TagDto.builder().id(2L).name("tag2").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(tag1, tag2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/tags?page=1&size=2&sort=name,asc&filter=name:whatever")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.tags", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "tag-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.tags[]").description("The elements"),
                                        fieldWithPath("_embedded.tags[].id").description("The id of the tag"),
                                        fieldWithPath("_embedded.tags[].name").description("The name of the tag"),
                                        fieldWithPath("_embedded.tags[].createdBy").description("Who created the tag"),
                                        fieldWithPath("_embedded.tags[].createdAt").description("When was the tag created"),
                                        fieldWithPath("_embedded.tags[].lastModifiedBy").description("Who last modified the tag"),
                                        fieldWithPath("_embedded.tags[].lastModifiedAt").description("When was the tag last modified"),
                                        subsectionWithPath("_embedded.tags[]._links").description("The tag links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TagApi.class, "retrieve", Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        when(jobService.createExportJob(any(Class.class), anyList(), any(String.class), any(ImpexType.class), any(Locale.class))).thenReturn(1L);

        mockMvc
                .perform(
                        get("/api/tags?ids=1,2,3&type=excel")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "tag-get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("type").description("The impex type (excel, excel_xls and pdf supported)"),
                                        parameterWithName("ids").description("The ids of the tags to export").optional(),
                                        parameterWithName("filter").description("The filter to use for the tags to export").optional()
                                ),
                                exportResponseFields,
                                security(getRolesFromMethod(TagApi.class, "retrieve", List.class, String.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(TagCreateDto.class);
        final var dto = TagCreateDto.builder().name("Tag").build();

        when(service.create(any(TagCreateDto.class))).thenReturn(TagDto.builder().id(1L).name(dto.name()).build());

        mockMvc
                .perform(
                        post("/api/tags")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "tag-create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the tag")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(TagApi.class, "create", TagCreateDto.class))
                        )
                );
    }

    @Test
    void should_create_import() throws Exception {
        when(jobService.createImportJob(any(), any(), any(ImpexType.class), any(Locale.class))).thenReturn(1L);

        mockMvc
                .perform(
                        post("/api/tags?type=excel")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .content(new byte[]{1, 2, 3})
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "tag-create-import",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("type").description("The impex type (excel, excel_xls and pdf supported)")
                                ),
                                importResponseFields,
                                security(getRolesFromMethod(TagApi.class, "createAndUpdate", byte[].class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var tag = TagDto.builder().id(1L).name("tag").build();

        when(service.findById(any(Long.class))).thenReturn(tag);

        mockMvc
                .perform(
                        get("/api/tags/{id}", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tag-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TagApi.class, "retrieve", Long.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(TagUpdateDto.class);
        final var tag = TagDto.builder().id(1L).name("tag").build();
        final var dto = TagUpdateDto.builder().id(1L).name("tag2").build();

        when(service.update(any(TagUpdateDto.class))).thenReturn(tag);

        mockMvc
                .perform(
                        put("/api/tags/{id}", 1)
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
                                "tag-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the tag"),
                                        fields.withPath("name").description("The name of the tag")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TagApi.class, "update", Long.class, TagUpdateDto.class))
                        )
                );
    }

    @Test
    void should_partial_update() throws Exception {
        final var fields = new ConstrainedFields(TagUpdateDto.class);
        final var tag = TagDto.builder().id(1L).name("tag").build();
        final var dto = TagUpdateDto.builder().id(1L).build();

        when(service.partialUpdate(any(TagUpdateDto.class))).thenReturn(tag);

        mockMvc
                .perform(
                        patch("/api/tags/{id}", 1)
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
                                "tag-update-partial",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the tag"),
                                        fields.withPath("name").description("The name of the tag").optional()
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TagApi.class, "partialUpdate", Long.class, TagUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        final var tag = TagDto.builder().id(1L).name("tag").build();

        when(service.findById(any(Long.class))).thenReturn(tag);
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/tags/{id}", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "tag-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(TagApi.class, "delete", Long.class))
                        )
                );
    }

}
