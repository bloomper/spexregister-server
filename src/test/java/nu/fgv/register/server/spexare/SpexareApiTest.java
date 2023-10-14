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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

@WebMvcTest(value = SpexareApi.class)
public class SpexareApiTest extends AbstractApiTest {

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
    public void should_get_paged() throws Exception {
        var spexare1 = SpexareDto.builder().id(1L).firstName("FirstName1").lastName("LastName1").build();
        var spexare2 = SpexareDto.builder().id(2L).firstName("FirstName2").lastName("LastName2").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spexare1, spexare2), PageRequest.of(1, 2, Sort.by("firstName")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare?page=1&size=2&sort=firstName,desc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spexare", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get-paged",
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
                                pagingQueryParameters,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_search_paged() throws Exception {
        var spexare1 = SpexareDto.builder().id(1L).firstName("FirstName1").lastName("LastName1").build();
        var spexare2 = SpexareDto.builder().id(2L).firstName("FirstName2").lastName("LastName2").build();
        var facets = List.of(Facet.builder().name("facet").values(Map.of("whatever", 2L)).build());
        var pageWithFacets = new PageWithFacetsImpl<>(List.of(spexare1, spexare2), PageRequest.of(1, 2, Sort.by("firstName")), SimpleSearchResultTotal.of(2, true), facets);
        var pageWithFacetsModel = PagedWithFacetsModel.of(
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
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spexare", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/search-paged",
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_export() throws Exception {
        var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        mockMvc
                .perform(
                        get("/api/v1/spexare?ids=1,2,3")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get-export",
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
                                responseBody()
                        )
                );
    }

    @Test
    public void should_create() throws Exception {
        var fields = new ConstrainedFields(SpexareCreateDto.class);
        var dto = SpexareCreateDto.builder().firstName("FirstName").lastName("LastName").build();

        when(service.create(any(SpexareCreateDto.class))).thenReturn(SpexareDto.builder().id(1L).firstName(dto.getFirstName()).lastName(dto.getLastName()).build());

        mockMvc
                .perform(
                        post("/api/v1/spexare")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/create",
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
                                createResponseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        var fields = new ConstrainedFields(SpexareUpdateDto.class);
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        var dto = SpexareUpdateDto.builder().id(1L).firstName("FirstName").lastName("LastName").nickName("NickName").build();

        when(service.update(any(SpexareUpdateDto.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/update",
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update() throws Exception {
        var fields = new ConstrainedFields(SpexUpdateDto.class);
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        var dto = SpexareUpdateDto.builder().id(1L).firstName("FirstName").lastName("LastName").nickName("NickName").build();

        when(service.partialUpdate(any(SpexareUpdateDto.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        patch("/api/v1/spexare/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/partial-update",
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spexare));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_download_image() throws Exception {
        var image = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);

        when(service.getImage(any(Long.class))).thenReturn(Optional.of(image));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}/image", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, image.getSecond()))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, image.getFirst().length))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-download",
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
                                responseBody()
                        )
                );
    }

    @Test
    public void should_upload_image() throws Exception {
        var image = new byte[]{10, 12};
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.saveImage(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{id}/image", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.IMAGE_PNG)
                                .content(image)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-upload",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (image/png, image/jpeg and image/gif supported)")
                                ),
                                requestBody()
                        )
                );
    }

    @Test
    public void should_upload_image_via_multipart() throws Exception {
        var image = new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.saveImage(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        multipart("/api/v1/spexare/{id}/image", 1L)
                                .file(image)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-upload-multipart",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders,
                                requestParts(
                                        partWithName("file").description("The image to upload")
                                )
                        )
                );
    }

    @Test
    public void should_delete_image() throws Exception {
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.deleteImage(any(Long.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{id}/image", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_get_partner() throws Exception {
        var partner = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findPartnerBySpexare(any(Long.class))).thenReturn(Optional.of(partner));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}/partner", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get-partner",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update_partner() throws Exception {
        var partner = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.updatePartner(any(Long.class), any(Long.class))).thenReturn(Optional.of(partner));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/partner/{id}", 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/partner-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the partner")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete_partner() throws Exception {
        when(service.deletePartner(any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/partner", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare/partner-delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_get_events() throws Exception {
        var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.SPEXARE.name()).build();
        var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.SPEXARE.name()).build();
        var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/spexare/events?sinceInDays=30")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get-events",
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
                                responseHeaders
                        )
                );
    }

}
