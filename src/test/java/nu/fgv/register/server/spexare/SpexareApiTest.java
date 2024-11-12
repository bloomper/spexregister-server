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

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.spex.SpexUpdateDto;
import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.PageWithFacets;
import nu.fgv.register.server.util.search.PageWithFacetsImpl;
import nu.fgv.register.server.util.search.PagedWithFacetsModel;
import nu.fgv.register.server.util.search.PagedWithFacetsResourcesAssembler;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

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
@WebMvcTest(value = SpexareApi.class)
class SpexareApiTest extends AbstractApiTest {

    @MockBean
    private SpexareService service;

    @MockBean
    private SpexareImportService importService;

    @MockBean
    private SpexareExportService exportService;

    @MockBean
    private EventService eventService;

    @MockBean
    private EventApi eventApi;

    @MockBean
    private PagedWithFacetsResourcesAssembler<SpexareDto> pagedWithFacetsResourcesAssembler; // must mock as it is not instantiated when using @WebMvcTest

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spexare"),
            fieldWithPath("firstName").description("The first name of the spexare"),
            fieldWithPath("lastName").description("The last name of the spexare"),
            fieldWithPath("nickName").description("The nickname of the spexare"),
            fieldWithPath("socialSecurityNumber").description("The social security number of the spexare"),
            fieldWithPath("graduation").description("The graduation of the spexare"),
            fieldWithPath("comment").description("The comment of the spexare"),
            fieldWithPath("image").description("The image of the spexare"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("image").description("Link to the current spexare's image").optional(),
            linkWithRel("activities").description("Link to the current spexare's activities").optional(),
            linkWithRel("memberships").description("Link to the current spexare's memberships").optional(),
            linkWithRel("consents").description("Link to the current spexare's consents").optional(),
            linkWithRel("toggles").description("Link to the current spexare's toggles").optional(),
            linkWithRel("addresses").description("Link to the current spexare's addresses").optional(),
            linkWithRel("tags").description("Link to the current spexare's tags").optional(),
            linkWithRel("partner").description("Link to the current spexare's partner").optional(),
            linkWithRel("events").description("Link to spexare events").optional()
    );

    @Test
    void should_get_paged() throws Exception {
        final var spexare1 = SpexareDto.builder().id(1L).firstName("FirstName1").lastName("LastName1").build();
        final var spexare2 = SpexareDto.builder().id(2L).firstName("FirstName2").lastName("LastName2").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spexare1, spexare2), PageRequest.of(1, 2, Sort.by("firstName")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare?page=1&size=2&sort=firstName,desc&filter=firstName:whatever")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spexare", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spexare[]").description("The elements"),
                                        fieldWithPath("_embedded.spexare[].id").description("The id of the spexare"),
                                        fieldWithPath("_embedded.spexare[].firstName").description("The first name of the spexare"),
                                        fieldWithPath("_embedded.spexare[].lastName").description("The last name of the spexare"),
                                        fieldWithPath("_embedded.spexare[].nickName").description("The nickname of the spexare"),
                                        fieldWithPath("_embedded.spexare[].image").description("The image of the spexare"),
                                        fieldWithPath("_embedded.spexare[].createdBy").description("Who created the spexare"),
                                        fieldWithPath("_embedded.spexare[].createdAt").description("When was the spexare created"),
                                        fieldWithPath("_embedded.spexare[].lastModifiedBy").description("Who last modified the spexare"),
                                        fieldWithPath("_embedded.spexare[].lastModifiedAt").description("When was the spexare last modified"),
                                        subsectionWithPath("_embedded.spexare[]._links").description("The spexare links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "retrieve", Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_search_paged() throws Exception {
        final var spexare1 = SpexareDto.builder().id(1L).firstName("FirstName1").lastName("LastName1").build();
        final var spexare2 = SpexareDto.builder().id(2L).firstName("FirstName2").lastName("LastName2").build();
        final var facets = List.of(Facet.builder().name("facet").values(Map.of("whatever", 2L)).build());
        final var pageWithFacets = new PageWithFacetsImpl<>(List.of(spexare1, spexare2), PageRequest.of(1, 2, Sort.by("firstName")), SimpleSearchResultTotal.of(2, true), facets);
        final var pageWithFacetsModel = PagedWithFacetsModel.of(
                pageWithFacets.stream().map(EntityModel::of).toList(),
                new PagedWithFacetsModel.PageMetadata(pageWithFacets.getSize(), pageWithFacets.getNumber(), pageWithFacets.getTotalElements(), pageWithFacets.getTotalPages()), pageWithFacets.getFacets()
        );
        pageWithFacetsModel.add(Link.of("https://whatever", IanaLinkRelations.FIRST));
        pageWithFacetsModel.add(Link.of("https://whatever", IanaLinkRelations.PREV));
        pageWithFacetsModel.add(Link.of("https://whatever", IanaLinkRelations.SELF));
        pageWithFacetsModel.add(Link.of("https://whatever", IanaLinkRelations.NEXT));
        pageWithFacetsModel.add(Link.of("https://whatever", IanaLinkRelations.LAST));

        when(service.search(any(String.class), any(Pageable.class))).thenReturn(pageWithFacets);
        when(pagedWithFacetsResourcesAssembler.toModel(any(PageWithFacets.class))).thenReturn(pageWithFacetsModel);

        mockMvc
                .perform(
                        get("/api/v1/spexare?q=FirstName&page=1&size=2&sort=firstName,desc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spexare", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-get-all-search",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spexare[]").description("The elements"),
                                        fieldWithPath("_embedded.spexare[].id").description("The id of the spexare"),
                                        fieldWithPath("_embedded.spexare[].firstName").description("The first name of the spexare"),
                                        fieldWithPath("_embedded.spexare[].lastName").description("The last name of the spexare"),
                                        fieldWithPath("_embedded.spexare[].nickName").description("The nickname of the spexare"),
                                        fieldWithPath("_embedded.spexare[].image").description("The image of the spexare"),
                                        fieldWithPath("_embedded.spexare[].createdBy").description("Who created the spexare"),
                                        fieldWithPath("_embedded.spexare[].createdAt").description("When was the spexare created"),
                                        fieldWithPath("_embedded.spexare[].lastModifiedBy").description("Who last modified the spexare"),
                                        fieldWithPath("_embedded.spexare[].lastModifiedAt").description("When was the spexare last modified"),
                                        subsectionWithPath("_embedded.spexare[]._links").description("The spexare links"),
                                        subsectionWithPath("_facets").description("The facets"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(
                                        parameterWithName("q").description("The query")
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "search", String.class, Pageable.class))
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        final var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        mockMvc
                .perform(
                        get("/api/v1/spexare?ids=1,2,3")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-get-all-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the spexare to export").optional()
                                ),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.ACCEPT).description("The content type (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet and application/vnd.ms-excel supported)")
                                ),
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody(),
                                security(getRolesFromMethod(SpexareApi.class, "retrieve", List.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(SpexareCreateDto.class);
        final var dto = SpexareCreateDto.builder().firstName("FirstName").lastName("LastName").build();

        when(service.create(any(SpexareCreateDto.class))).thenReturn(SpexareDto.builder().id(1L).firstName(dto.getFirstName()).lastName(dto.getLastName()).build());

        mockMvc
                .perform(
                        post("/api/v1/spexare")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare-create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("firstName").description("The first name of the spexare"),
                                        fields.withPath("lastName").description("The last name of the spexare"),
                                        fields.withPath("nickName").description("The nickname of the spexare")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "create", SpexareCreateDto.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findById(any(Long.class))).thenReturn(spexare);

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "retrieve", Long.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(SpexareUpdateDto.class);
        final var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        final var dto = SpexareUpdateDto.builder().id(1L).firstName("FirstName").lastName("LastName").nickName("NickName").build();

        when(service.update(any(SpexareUpdateDto.class))).thenReturn(spexare);

        mockMvc
                .perform(
                        put("/api/v1/spexare/{id}", 1L)
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
                                "spexare-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spexare"),
                                        fields.withPath("firstName").description("The first name of the spexare"),
                                        fields.withPath("lastName").description("The last name of the spexare"),
                                        fields.withPath("nickName").description("The nickname of the spexare"),
                                        fields.withPath("socialSecurityNumber").description("The social security number of the spexare"),
                                        fields.withPath("graduation").description("The graduation of the spexare"),
                                        fields.withPath("comment").description("The comment of the spexare")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "update", Long.class, SpexareUpdateDto.class))
                        )
                );
    }

    @Test
    void should_partial_update() throws Exception {
        final var fields = new ConstrainedFields(SpexUpdateDto.class);
        final var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        final var dto = SpexareUpdateDto.builder().id(1L).firstName("FirstName").lastName("LastName").nickName("NickName").build();

        when(service.partialUpdate(any(SpexareUpdateDto.class))).thenReturn(spexare);

        mockMvc
                .perform(
                        patch("/api/v1/spexare/{id}", 1L)
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
                                "spexare-update-partial",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spexare"),
                                        fields.withPath("firstName").description("The first name of the spexare").optional(),
                                        fields.withPath("lastName").description("The last name of the spexare").optional(),
                                        fields.withPath("nickName").description("The nickname of the spexare").optional(),
                                        fields.withPath("socialSecurityNumber").description("The social security number of the spexare").optional(),
                                        fields.withPath("graduation").description("The graduation of the spexare").optional(),
                                        fields.withPath("comment").description("The comment of the spexare").optional()
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "partialUpdate", Long.class, SpexareUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        final var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findById(any(Long.class))).thenReturn(spexare);
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "delete", Long.class))
                        )
                );
    }

    @Test
    void should_download_image() throws Exception {
        final var image = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);

        when(service.getImage(any(Long.class))).thenReturn(image);

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}/image", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, image.getSecond()))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, image.getFirst().length))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-image-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders,
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody(),
                                security(getRolesFromMethod(SpexareApi.class, "downloadImage", Long.class))
                        )
                );
    }

    @Test
    void should_upload_image() throws Exception {
        final var image = new byte[]{10, 12};
        final var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.saveImage(any(Long.class), any(), any(String.class))).thenReturn(spexare);

        mockMvc
                .perform(
                        put("/api/v1/spexare/{id}/image", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.IMAGE_PNG)
                                .content(image)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare-image-add",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (image/png, image/jpeg and image/gif supported)")
                                ),
                                requestBody(),
                                security(getRolesFromMethod(SpexareApi.class, "uploadImage", Long.class, byte[].class, String.class))
                        )
                );
    }

    @Test
    void should_upload_image_via_multipart() throws Exception {
        final var image = new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        final var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.saveImage(any(Long.class), any(), any(String.class))).thenReturn(spexare);

        mockMvc
                .perform(
                        multipart("/api/v1/spexare/{id}/image", 1L)
                                .file(image)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare-image-add-multipart",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders,
                                requestParts(
                                        partWithName("file").description("The image to upload")
                                ),
                                security(getRolesFromMethod(SpexareApi.class, "uploadImage", Long.class, MultipartFile.class))
                        )
                );
    }

    @Test
    void should_delete_image() throws Exception {
        final var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.deleteImage(any(Long.class))).thenReturn(spexare);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{id}/image", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare-image-remove",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "deleteImage", Long.class))
                        )
                );
    }

    @Test
    void should_get_partner() throws Exception {
        final var partner = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findPartnerBySpexare(any(Long.class))).thenReturn(partner);

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}/partner", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-partner-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "retrievePartner", Long.class))
                        )
                );
    }

    @Test
    void should_update_partner() throws Exception {
        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/partner/{id}", 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare-partner-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the partner")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "updatePartner", Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_delete_partner() throws Exception {
        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/partner", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare-partner-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(SpexareApi.class, "deletePartner", Long.class))
                        )
                );
    }

    @Test
    void should_get_events() throws Exception {
        final var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.SPEXARE.name()).build();
        final var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.SPEXARE.name()).build();
        final var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/spexare/events?sinceInDays=30")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare-event-get",
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
                                security(getRolesFromMethod(SpexareApi.class, "retrieveEvents", Integer.class))
                        )
                );
    }

}
