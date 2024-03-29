package nu.fgv.register.server.spex;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.spex.category.SpexCategoryApi;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import java.util.List;
import java.util.Locale;
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

@WebMvcTest(value = SpexApi.class)
class SpexApiTest extends AbstractApiTest {

    @MockBean
    private SpexService service;

    @MockBean
    private SpexImportService importService;

    @MockBean
    private SpexExportService exportService;

    @MockBean
    private SpexCategoryApi categoryApi;

    @MockBean
    private EventService eventService;

    @MockBean
    private EventApi eventApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex"),
            fieldWithPath("year").description("The year of the spex"),
            fieldWithPath("title").description("The title of the spex"),
            fieldWithPath("revival").description("The revival flag of the spex"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("poster").description("Link to the current spex's poster").optional(),
            linkWithRel("parent").description("Link to the current spex's parent").optional(),
            linkWithRel("revivals").description("Link to the current spex's revivals").optional(),
            linkWithRel("category").description("Link to the current spex's spex category").optional(),
            linkWithRel("spex").description("Link to paged spex").optional(),
            linkWithRel("spex-including-revivals").description("Link to paged spex (including revivals)").optional(),
            linkWithRel("events").description("Link to spex events").optional()
    );

    private final ResponseFieldsSnippet categoryResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex category"),
            fieldWithPath("name").description("The name of the spex category"),
            fieldWithPath("firstYear").description("The first year of the spex category"),
            linksSubsection
    );

    private final LinksSnippet categoryLinks = baseLinks.and(
            linkWithRel("spex-categories").description("Link to paged spex categories").optional(),
            linkWithRel("logo").description("Link to the current spex category's logo").optional(),
            linkWithRel("events").description("Link to spex category events").optional()
    );

    @Test
    void should_get_paged() throws Exception {
        var spex1 = SpexDto.builder().id(1L).year("2021").build();
        var spex2 = SpexDto.builder().id(2L).year("2022").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spex1, spex2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spex?page=1&size=2&sort=year,desc&filter=parent:NULL")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex/get-paged",
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
                                responseHeaders
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        mockMvc
                .perform(
                        get("/api/v1/spex?ids=1,2,3")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "spex/get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the spex to export").optional()
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
    void should_create() throws Exception {
        var fields = new ConstrainedFields(SpexCreateDto.class);
        var dto = SpexCreateDto.builder().year("1948").title("Bojan").build();

        when(service.create(any(SpexCreateDto.class))).thenReturn(SpexDto.builder().id(1L).year(dto.getYear()).title(dto.getTitle()).build());

        mockMvc
                .perform(
                        post("/api/v1/spex")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("year").description("The year of the spex"),
                                        fields.withPath("title").description("The title of the spex")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        var spex = SpexDto.builder().id(1L).year("2021").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        get("/api/v1/spex/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        var fields = new ConstrainedFields(SpexUpdateDto.class);
        var spex = SpexDto.builder().id(1L).year("2021").build();
        var dto = SpexUpdateDto.builder().id(1L).year("1948").title("Bojan").build();

        when(service.update(any(SpexUpdateDto.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        put("/api/v1/spex/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spex"),
                                        fields.withPath("year").description("The year of the spex"),
                                        fields.withPath("title").description("The title of the spex")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    void should_partial_update() throws Exception {
        var fields = new ConstrainedFields(SpexUpdateDto.class);
        var spex = SpexDto.builder().id(1L).year("2021").build();
        var dto = SpexUpdateDto.builder().id(1L).year("1948").title("Bojan").build();

        when(service.partialUpdate(any(SpexUpdateDto.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        patch("/api/v1/spex/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex/partial-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spex"),
                                        fields.withPath("year").description("The year of the spex").optional(),
                                        fields.withPath("title").description("The title of the spex").optional()
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        var spex = SpexDto.builder().id(1L).year("2021").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spex));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/spex/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    void should_download_poster() throws Exception {
        var poster = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);
        when(service.getPoster(any(Long.class))).thenReturn(Optional.of(poster));

        mockMvc
                .perform(
                        get("/api/v1/spex/{id}/poster", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, poster.getSecond()))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, poster.getFirst().length))
                .andDo(print())
                .andDo(
                        document(
                                "spex/poster-download",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
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
    void should_upload_poster() throws Exception {
        var poster = new byte[]{10, 12};
        var spex = SpexDto.builder().id(1L).year("2021").build();
        when(service.savePoster(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        put("/api/v1/spex/{id}/poster", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.IMAGE_PNG)
                                .content(poster)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/poster-upload",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (image/png, image/jpeg and image/gif supported)")
                                ),
                                requestBody()
                        )
                );
    }

    @Test
    void should_upload_poster_via_multipart() throws Exception {
        var poster = new MockMultipartFile("file", "poster.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        var spex = SpexDto.builder().id(1L).year("2021").build();
        when(service.savePoster(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        multipart("/api/v1/spex/{id}/poster", 1L)
                                .file(poster)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/poster-upload-multipart",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                secureRequestHeaders,
                                requestParts(
                                        partWithName("file").description("The poster to upload")
                                )
                        )
                );
    }

    @Test
    void should_delete_poster() throws Exception {
        var spex = SpexDto.builder().id(1L).year("2021").build();
        when(service.deletePoster(any(Long.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        delete("/api/v1/spex/{id}/poster", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/poster-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    void should_get_revival() throws Exception {
        var revival = SpexDto.builder().id(1L).year("2021").build();

        when(service.findRevivalById(any(Long.class), any(Long.class))).thenReturn(Optional.of(revival));

        mockMvc
                .perform(
                        get("/api/v1/spex/{spexId}/revivals/{id}", 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex/revivals-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex"),
                                        parameterWithName("id").description("The id of the revival")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    void should_get_paged_revivals() throws Exception {
        var revival1 = SpexDto.builder().id(1L).year("2021").revival(true).build();
        var revival2 = SpexDto.builder().id(1L).year("2022").revival(true).build();

        when(service.findRevivals(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(revival1, revival2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spex/revivals?page=1&size=2&sort=year,desc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex/revivals-paged",
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
                                pagingQueryParameters,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    void should_get_paged_specific_revivals() throws Exception {
        var revival1 = SpexDto.builder().id(1L).year("2021").revival(true).build();
        var revival2 = SpexDto.builder().id(1L).year("2022").revival(true).build();

        when(service.findRevivalsByParent(any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(revival1, revival2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spex/{spexId}/revivals?page=1&size=2&sort=year,desc", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex/revivals-specific-paged",
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
                                responseHeaders
                        )
                );
    }

    @Test
    void should_add_revival() throws Exception {
        var revival = SpexDto.builder().id(1L).year("2021").title("Bojan").revival(true).build();

        when(service.addRevival(any(Long.class), any(String.class))).thenReturn(Optional.of(revival));

        mockMvc
                .perform(
                        post("/api/v1/spex/{spexId}/revivals/{year}", 1L, "2021")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex/revival-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex"),
                                        parameterWithName("year").description("The year of the revival")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders
                        )
                );
    }

    @Test
    void should_delete_revival() throws Exception {
        when(service.deleteRevival(any(Long.class), any(String.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spex/{spexId}/revivals/{year}", 1L, "2021")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spex/revival-delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex"),
                                        parameterWithName("year").description("The year of the revival")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    void should_get_category() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").build();
        var realCategoryApi = new SpexCategoryApi(null, null, null, null, null, null);

        when(service.findCategoryBySpex(any(Long.class))).thenReturn(Optional.of(category));
        when(categoryApi.getLinks(any(SpexCategoryDto.class))).thenReturn(realCategoryApi.getLinks(category));

        mockMvc
                .perform(
                        get("/api/v1/spex/{spexId}/category", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex/category-get",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex")
                                ),
                                categoryResponseFields,
                                categoryLinks,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    void should_add_category() throws Exception {
        when(service.addCategory(any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        put("/api/v1/spex/{spexId}/category/{id}", 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isAccepted())
                .andDo(document(
                                "spex/category-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex"),
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    void should_remove_category() throws Exception {
        when(service.removeCategory(any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spex/{spexId}/category", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spex/category-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    void should_get_events() throws Exception {
        var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.SPEX.name()).build();
        var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.SPEX.name()).build();
        var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/spex/events?sinceInDays=30")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex/get-events",
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
