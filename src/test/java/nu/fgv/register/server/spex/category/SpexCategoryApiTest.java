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

package nu.fgv.register.server.spex.category;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
@WebMvcTest(value = SpexCategoryApi.class)
class SpexCategoryApiTest extends AbstractApiTest {

    @MockitoBean
    private SpexCategoryService service;

    @MockitoBean
    private SpexCategoryImportService importService;

    @MockitoBean
    private SpexCategoryExportService exportService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventApi eventApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex category"),
            fieldWithPath("name").description("The name of the spex category"),
            fieldWithPath("firstYear").description("The first year of the spex category"),
            fieldWithPath("logoUrl").description("The logo URL of the spex category"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spex-categories").description("Link to paged spex categories").optional(),
            linkWithRel("logo").description("Link to the current spex category's logo").optional(),
            linkWithRel("events").description("Link to spex category events").optional()
    );

    @Test
    void should_get_paged() throws Exception {
        final var category1 = SpexCategoryDto.builder().id(1L).name("category1").build();
        final var category2 = SpexCategoryDto.builder().id(2L).name("category2").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category1, category2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/spex/categories?page=1&size=2&sort=name,asc&filter=name:whatever")
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex-categories", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spex-categories[]").description("The elements"),
                                        fieldWithPath("_embedded.spex-categories[].id").description("The id of the spex category"),
                                        fieldWithPath("_embedded.spex-categories[].name").description("The name of the spex category"),
                                        fieldWithPath("_embedded.spex-categories[].firstYear").description("The first year of the spex category"),
                                        fieldWithPath("_embedded.spex-categories[].createdBy").description("Who created the spex category"),
                                        fieldWithPath("_embedded.spex-categories[].createdAt").description("When was the spex category created"),
                                        fieldWithPath("_embedded.spex-categories[].lastModifiedBy").description("Who last modified the spex category"),
                                        fieldWithPath("_embedded.spex-categories[].lastModifiedAt").description("When was the spex category last modified"),
                                        subsectionWithPath("_embedded.spex-categories[]._links").description("The spex category links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "retrieve", Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        final var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        mockMvc
                .perform(
                        get("/api/spex/categories?ids=1,2,3")
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-get-all-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the spex categories to export").optional()
                                ),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.ACCEPT).description("The content type (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet and application/vnd.ms-excel supported)")
                                ),
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody(),
                                security(getRolesFromMethod(SpexCategoryApi.class, "retrieve", List.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(SpexCategoryCreateDto.class);
        final var dto = SpexCategoryCreateDto.builder().firstYear("1948").name("Chalmersspexet").build();

        when(service.create(any(SpexCategoryCreateDto.class))).thenReturn(SpexCategoryDto.builder().id(1L).firstYear(dto.firstYear()).name(dto.name()).build());

        mockMvc
                .perform(
                        post("/api/spex/categories")
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex-category-create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the spex category"),
                                        fields.withPath("firstYear").description("The first year of the spex category")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "create", SpexCategoryCreateDto.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var category = SpexCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(category);

        mockMvc
                .perform(
                        get("/api/spex/categories/{id}", 1)
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-get-2",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "retrieve", Long.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(SpexCategoryUpdateDto.class);
        final var category = SpexCategoryDto.builder().id(1L).name("category").build();
        final var dto = SpexCategoryUpdateDto.builder().id(1L).firstYear("1948").name("Chalmersspexet").build();

        when(service.update(any(SpexCategoryUpdateDto.class))).thenReturn(category);

        mockMvc
                .perform(
                        put("/api/spex/categories/{id}", 1)
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
                                "spex-category-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spex category"),
                                        fields.withPath("name").description("The name of the spex category"),
                                        fields.withPath("firstYear").description("The first year of the spex category")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "update", Long.class, SpexCategoryUpdateDto.class))
                        )
                );
    }

    @Test
    void should_partial_update() throws Exception {
        final var fields = new ConstrainedFields(SpexCategoryUpdateDto.class);
        final var category = SpexCategoryDto.builder().id(1L).name("category").build();
        final var dto = SpexCategoryUpdateDto.builder().id(1L).firstYear("1948").build();

        when(service.partialUpdate(any(SpexCategoryUpdateDto.class))).thenReturn(category);

        mockMvc
                .perform(
                        patch("/api/spex/categories/{id}", 1)
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
                                "spex-category-update-partial",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spex category"),
                                        fields.withPath("name").description("The name of the spex category").optional(),
                                        fields.withPath("firstYear").description("The first year of the spex category").optional()
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "partialUpdate", Long.class, SpexCategoryUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        final var category = SpexCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(category);
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/spex/categories/{id}", 1)
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "delete", Long.class))
                        )
                );
    }

    @Test
    void should_download_logo() throws Exception {
        final var logo = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);
        when(service.getLogo(any(Long.class))).thenReturn(logo);

        mockMvc
                .perform(
                        get("/api/spex/categories/{spexId}/logo", 1)
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, logo.getSecond()))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, logo.getFirst().length))
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-logo-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
                                ),
                                secureRequestHeaders,
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody(),
                                security(getRolesFromMethod(SpexCategoryApi.class, "downloadLogo", Long.class))
                        )
                );
    }

    @Test
    void should_upload_logo() throws Exception {
        final var logo = new byte[]{10, 12};
        final var category = SpexCategoryDto.builder().id(1L).name("category").build();
        when(service.saveLogo(any(Long.class), any(), any(String.class))).thenReturn(category);

        mockMvc
                .perform(
                        put("/api/spex/categories/{spexId}/logo", 1)
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.IMAGE_PNG)
                                .content(logo)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-logo-add",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
                                ),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (image/png, image/jpeg and image/gif supported)")
                                ),
                                requestBody(),
                                security(getRolesFromMethod(SpexCategoryApi.class, "uploadLogo", Long.class, byte[].class, String.class))
                        )
                );
    }

    @Test
    void should_upload_logo_via_multipart() throws Exception {
        final var logo = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        final var category = SpexCategoryDto.builder().id(1L).name("category").build();
        when(service.saveLogo(any(Long.class), any(), any(String.class))).thenReturn(category);

        mockMvc
                .perform(
                        multipart("/api/spex/categories/{spexId}/logo", 1)
                                .file(logo)
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-logo-add-multipart",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
                                ),
                                secureRequestHeaders,
                                requestParts(
                                        partWithName("file").description("The logo to upload")
                                ),
                                security(getRolesFromMethod(SpexCategoryApi.class, "uploadLogo", Long.class, MultipartFile.class))
                        )
                );
    }

    @Test
    void should_delete_logo() throws Exception {
        final var category = SpexCategoryDto.builder().id(1L).name("category").build();
        when(service.deleteLogo(any(Long.class))).thenReturn(category);

        mockMvc
                .perform(
                        delete("/api/spex/categories/{spexId}/logo", 1)
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-logo-remove",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "deleteLogo", Long.class))
                        )
                );
    }

    @Test
    void should_get_events() throws Exception {
        final var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.SPEX_CATEGORY.name()).build();
        final var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.SPEX_CATEGORY.name()).build();
        final var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/spex/categories/events?sinceInDays=30")
                                .apiVersion("1.0")
.header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex-category-event-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.events[]").description("The elements"),
                                        fieldWithPath("_embedded.events[].id").description("The id of the event"),
                                        fieldWithPath("_embedded.events[].event").description("The type of the event"),
                                        fieldWithPath("_embedded.events[].source").description("The source of the event"),
                                        fieldWithPath("_embedded.events[].createdBy").description("Who created the event"),
                                        fieldWithPath("_embedded.events[].createdAt").description("When was the event created"),
                                        subsectionWithPath("_embedded.events[]._links").description("The event links"),
                                        linksSubsection
                                ),
                                queryParameters(parameterWithName("sinceInDays").description("How many days back to check for events")),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexCategoryApi.class, "retrieveEvents", Integer.class))
                        )
                );
    }

}
