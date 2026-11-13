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

package nu.fgv.register.server.spex;

import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.spex.category.SpexCategoryApi;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.FileUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static nu.fgv.register.server.util.TestUuids.uuid;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestBody;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = SpexApi.class)
class SpexApiTest extends AbstractApiTest {

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex"),
            fieldWithPath("year").description("The year of the spex"),
            fieldWithPath("title").description("The title of the spex"),
            fieldWithPath("revival").description("The revival flag of the spex"),
            fieldWithPath("posterUrl").description("The poster URL of the spex"),
            fieldWithPath("retired").description("Whether the record is retired in Spexipedia: kept on activities, not offered for new ones"),
            linksSubsection
    );
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("poster").description("Link to the current spex's poster").optional(),
            linkWithRel("parent").description("Link to the current spex's parent").optional(),
            linkWithRel("revivals").description("Link to the current spex's revivals").optional(),
            linkWithRel("category").description("Link to the current spex's spex category").optional(),
            linkWithRel("spex").description("Link to paged spex").optional(),
            linkWithRel("spex-including-revivals").description("Link to paged spex (including revivals)").optional(),
            linkWithRel("revisions").description("Link to revisions").optional()
    );
    private final ResponseFieldsSnippet categoryResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex category"),
            fieldWithPath("name").description("The name of the spex category"),
            fieldWithPath("firstYear").description("The first year of the spex category"),
            fieldWithPath("logoUrl").description("The logo URL of the spex category"),
            fieldWithPath("retired").description("Whether the record is retired in Spexipedia: kept on activities, not offered for new ones"),
            linksSubsection
    );
    private final LinksSnippet categoryLinks = baseLinks.and(
            linkWithRel("spex-categories").description("Link to paged spex categories").optional(),
            linkWithRel("logo").description("Link to the current spex category's logo").optional(),
            linkWithRel("revisions").description("Link to revisions").optional()
    );
    @MockitoBean
    private SpexService service;
    @MockitoBean
    private SpexCategoryApi categoryApi;
    @MockitoBean
    private JobService jobService;

    @Test
    void should_get_paged() throws Exception {
        final var spex1 = SpexDto.builder().id(uuid(1)).year("2021").build();
        final var spex2 = SpexDto.builder().id(uuid(2)).year("2022").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spex1, spex2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/spex?page=1&size=2&sort=year,desc&filter=parent:NULL")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spex[]").description("The elements"),
                                        fieldWithPath("_embedded.spex[].id").description("The id of the spex"),
                                        fieldWithPath("_embedded.spex[].title").description("The title of the spex"),
                                        fieldWithPath("_embedded.spex[].year").description("The year of the spex"),
                                        fieldWithPath("_embedded.spex[].revival").description("If the spex is a revival"),
                                        fieldWithPath("_embedded.spex[].createdBy").description("Who created the spex"),
                                        fieldWithPath("_embedded.spex[].createdAt").description("When was the spex created"),
                                        fieldWithPath("_embedded.spex[].lastModifiedBy").description("Who last modified the spex"),
                                        fieldWithPath("_embedded.spex[].lastModifiedAt").description("When was the spex last modified"),
                                        subsectionWithPath("_embedded.spex[]._links").description("The spex links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexApi.class, "retrieve", Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        when(jobService.createExportJob(any(Class.class), anyList(), any(String.class), any(ImpexType.class), any(Locale.class))).thenReturn(1L);

        mockMvc
                .perform(
                        get("/api/spex?ids=" + uuid(1) + "," + uuid(2) + "," + uuid(3) + "&type=excel")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "spex-get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("type").description("The impex type (excel, excel_xls and pdf supported)"),
                                        parameterWithName("ids").description("The ids of the spex to export").optional(),
                                        parameterWithName("filter").description("The filter to use for the spex to export").optional()
                                ),
                                exportResponseFields,
                                security(getRolesFromMethod(SpexApi.class, "retrieve", List.class, String.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var spex = SpexDto.builder().id(uuid(1)).year("2021").build();

        when(service.findById(any(UUID.class))).thenReturn(spex);

        mockMvc
                .perform(
                        get("/api/spex/{id}", uuid(1))
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexApi.class, "retrieve", UUID.class))
                        )
                );
    }

    @Test
    void should_get_parent() throws Exception {
        final var spex = SpexDto.builder().id(uuid(1)).year("2021").build();

        when(service.findParentById(any(UUID.class))).thenReturn(spex);

        mockMvc
                .perform(
                        get("/api/spex/{spexId}/parent", uuid(1))
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex-parent-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexApi.class, "retrieveParent", UUID.class))
                        )
                );
    }

    @Test
    void should_get_revival() throws Exception {
        final var revival = SpexDto.builder().id(uuid(1)).year("2021").build();

        when(service.findRevivalById(any(UUID.class), any(UUID.class))).thenReturn(revival);

        mockMvc
                .perform(
                        get("/api/spex/{spexId}/revivals/{id}", uuid(1), uuid(1))
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex-revival-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex"),
                                        parameterWithName("id").description("The id of the revival")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexApi.class, "retrieveRevival", UUID.class, UUID.class))
                        )
                );
    }

    @Test
    void should_get_revivals() throws Exception {
        final var revival1 = SpexDto.builder().id(uuid(1)).year("2021").revival(true).build();
        final var revival2 = SpexDto.builder().id(uuid(1)).year("2022").revival(true).build();

        when(service.findRevivalsByParent(any(UUID.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(revival1, revival2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/spex/{spexId}/revivals?page=1&size=2&sort=year,desc", uuid(1))
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex-revival-get-2",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spex[]").description("The elements"),
                                        fieldWithPath("_embedded.spex[].id").description("The id of the spex"),
                                        fieldWithPath("_embedded.spex[].title").description("The title of the spex"),
                                        fieldWithPath("_embedded.spex[].year").description("The year of the spex"),
                                        fieldWithPath("_embedded.spex[].revival").description("If the spex is a revival"),
                                        fieldWithPath("_embedded.spex[].createdBy").description("Who created the spex"),
                                        fieldWithPath("_embedded.spex[].createdAt").description("When was the spex created"),
                                        fieldWithPath("_embedded.spex[].lastModifiedBy").description("Who last modified the spex"),
                                        fieldWithPath("_embedded.spex[].lastModifiedAt").description("When was the spex last modified"),
                                        subsectionWithPath("_embedded.spex[]._links").description("The spex links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexApi.class, "retrieveRevivalsByParent", UUID.class, Pageable.class))
                        )
                );
    }

    @Test
    void should_get_category() throws Exception {
        final var category = SpexCategoryDto.builder().id(uuid(1)).name("category").build();
        final var realCategoryApi = new SpexCategoryApi(null, null, null);

        when(service.findCategoryBySpex(any(UUID.class))).thenReturn(category);
        when(categoryApi.getLinks(any(SpexCategoryDto.class))).thenReturn(realCategoryApi.getLinks(category));

        mockMvc
                .perform(
                        get("/api/spex/{spexId}/category", uuid(1))
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex-category-get",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex")
                                ),
                                categoryResponseFields,
                                categoryLinks,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexApi.class, "retrieveCategory", UUID.class))
                        )
                );
    }

}
