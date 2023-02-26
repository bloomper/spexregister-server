package nu.fgv.register.server.spex;

import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SpexApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SpexApiTest extends AbstractApiTest {

    @MockBean
    private SpexService service;

    @MockBean
    private SpexImportService importService;
    @MockBean
    private SpexExportService exportService;

    @MockBean
    private SpexCategoryApi categoryApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex"),
            fieldWithPath("year").description("The year of the spex"),
            fieldWithPath("title").description("The title of the spex"),
            fieldWithPath("poster").description("The poster of the spex").optional(),
            fieldWithPath("category").description("The spex category of the spex").optional(),
            fieldWithPath("parent").description("The parent of the spex (if revival)").optional(),
            fieldWithPath("revival").description("The revival flag of the spex"),
            linksSubsection
    );

    private final ResponseFieldsSnippet responseFieldsWithCategory = responseFields.and(
            fieldWithPath("category.id").description("The id of the spex category"),
            fieldWithPath("category.name").description("The name of the spex category"),
            fieldWithPath("category.firstYear").description("The first year of the spex category"),
            fieldWithPath("category.logo").description("The logo of the spex category").optional(),
            fieldWithPath("category.createdBy").description("Who created the entity"),
            fieldWithPath("category.createdAt").description("When was the entity created"),
            fieldWithPath("category.lastModifiedBy").description("Who last modified the entity"),
            fieldWithPath("category.lastModifiedAt").description("When was the entity last modified")
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("poster").description("Link to the current spex's poster").optional(),
            linkWithRel("revivals").description("Link to the current spex's revivals").optional(),
            linkWithRel("category").description("Link to the current spex's spex category").optional()
    );

    @Test
    public void should_get_paged_spex() throws Exception {
        var spex1 = SpexDto.builder().id(1L).year("2021").build();
        var spex2 = SpexDto.builder().id(2L).year("2022").build();

        when(service.find(eq(false), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spex1, spex2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spex?page=1&size=2&sort=year,desc")
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
                                        fieldWithPath("_embedded.spex[].poster").description("The poster of the spex"),
                                        fieldWithPath("_embedded.spex[].category").description("The category of the spex"),
                                        fieldWithPath("_embedded.spex[].parent").description("The parent of the spex (if revival)"),
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_spex_export() throws Exception {
        var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        mockMvc
                .perform(
                        get("/api/v1/spex?ids=1,2,3")
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
                                requestHeaders(
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
    public void should_create_spex() throws Exception {
        var fields = new ConstrainedFields(SpexCreateDto.class);
        var dto = SpexCreateDto.builder().year("1948").title("Bojan").build();

        when(service.create(any(SpexCreateDto.class))).thenReturn(SpexDto.builder().id(1L).year(dto.getYear()).title(dto.getTitle()).build());

        mockMvc
                .perform(
                        post("/api/v1/spex")
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_spex() throws Exception {
        var spex = SpexDto.builder().id(1L).year("2021").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        get("/api/v1/spex/{id}", 1)
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update_spex() throws Exception {
        var fields = new ConstrainedFields(SpexUpdateDto.class);
        var spex = SpexDto.builder().id(1L).year("2021").build();
        var dto = SpexUpdateDto.builder().id(1L).year("1948").title("Bojan").build();

        when(service.update(any(SpexUpdateDto.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        put("/api/v1/spex/{id}", 1)
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update_spex() throws Exception {
        var fields = new ConstrainedFields(SpexUpdateDto.class);
        var spex = SpexDto.builder().id(1L).year("2021").build();
        var dto = SpexUpdateDto.builder().id(1L).year("1948").title("Bojan").build();

        when(service.partialUpdate(any(SpexUpdateDto.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        patch("/api/v1/spex/{id}", 1)
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete_spex() throws Exception {
        var spex = SpexDto.builder().id(1L).year("2021").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spex));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/spex/{id}", 1)
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
                                )
                        )
                );
    }

    @Test
    public void should_download_spex_poster() throws Exception {
        var poster = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);
        when(service.getPoster(any(Long.class))).thenReturn(Optional.of(poster));

        mockMvc
                .perform(
                        get("/api/v1/spex/{id}/poster", 1)
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
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody()
                        )
                );
    }

    @Test
    public void should_upload_spex_poster() throws Exception {
        var poster = new byte[]{10, 12};
        var spex = SpexDto.builder().id(1L).year("2021").build();
        when(service.savePoster(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        put("/api/v1/spex/{id}/poster", 1)
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
                                requestHeaders(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (image/png, image/jpeg and image/gif supported)")
                                ),
                                requestBody()
                        )
                );
    }

    @Test
    public void should_upload_spex_poster_via_multipart() throws Exception {
        var poster = new MockMultipartFile("file", "poster.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        var spex = SpexDto.builder().id(1L).year("2021").build();
        when(service.savePoster(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        multipart("/api/v1/spex/{id}/poster", 1)
                                .file(poster)
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
                                requestParts(
                                        partWithName("file").description("The poster to upload")
                                )
                        )
                );
    }

    @Test
    public void should_delete_spex_poster() throws Exception {
        var spex = SpexDto.builder().id(1L).year("2021").build();
        when(service.removePoster(any(Long.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        delete("/api/v1/spex/{id}/poster", 1)
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
                                )
                        )
                );
    }

    @Test
    public void should_get_paged_spex_revivals() throws Exception {
        var revival1 = SpexDto.builder().id(1L).year("2021").revival(true).build();
        var revival2 = SpexDto.builder().id(1L).year("2022").revival(true).build();

        when(service.findRevivals(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(revival1, revival2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spex/revivals?page=1&size=2&sort=year,desc")
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
                                        fieldWithPath("_embedded.spex[].poster").description("The poster of the spex"),
                                        fieldWithPath("_embedded.spex[].category").description("The category of the spex"),
                                        fieldWithPath("_embedded.spex[].parent").description("The parent of the spex (if revival)"),
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_paged_specific_spex_revivals() throws Exception {
        var revival1 = SpexDto.builder().id(1L).year("2021").revival(true).build();
        var revival2 = SpexDto.builder().id(1L).year("2022").revival(true).build();

        when(service.findRevivalsByParent(any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(revival1, revival2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spex/{id}/revivals?page=1&size=2&sort=year,desc", 1)
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
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spex[]").description("The elements"),
                                        fieldWithPath("_embedded.spex[].id").description("The id of the spex"),
                                        fieldWithPath("_embedded.spex[].title").description("The title of the spex"),
                                        fieldWithPath("_embedded.spex[].year").description("The year of the spex"),
                                        fieldWithPath("_embedded.spex[].poster").description("The poster of the spex"),
                                        fieldWithPath("_embedded.spex[].category").description("The category of the spex"),
                                        fieldWithPath("_embedded.spex[].parent").description("The parent of the spex (if revival)"),
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_add_spex_revival() throws Exception {
        var revival = SpexDto.builder().id(1L).year("2021").title("Bojan").revival(true).build();

        when(service.addRevival(any(Long.class), any(String.class))).thenReturn(Optional.of(revival));

        mockMvc
                .perform(
                        put("/api/v1/spex/{id}/revivals/{year}", 1, "2021")
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex/revival-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex"),
                                        parameterWithName("year").description("The year of the revival")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_remove_spex_revival() throws Exception {
        when(service.removeRevival(any(Long.class), any(String.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spex/{id}/revivals/{year}", 1, "2021")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spex/revival-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex"),
                                        parameterWithName("year").description("The year of the revival")
                                )
                        )
                );
    }

    @Test
    public void should_update_spex_spex_category() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").logo("logo").build();
        var spex = SpexDto.builder().id(1L).year("1948").title("Bojan").category(category).build();

        when(service.updateCategory(any(Long.class), any(Long.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        put("/api/v1/spex/{id}/spex-category/{categoryId}", 1, 1)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex/spex-category-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex"),
                                        parameterWithName("categoryId").description("The id of the spex category")
                                ),
                                responseFieldsWithCategory,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete_spex_spex_category() throws Exception {
        var spex = SpexDto.builder().id(1L).year("1948").title("Bojan").build();

        when(service.removeCategory(any(Long.class))).thenReturn(Optional.of(spex));

        mockMvc
                .perform(
                        delete("/api/v1/spex/{id}/spex-category", 1)
                )
                .andExpect(status().isAccepted())
                .andDo(document(
                                "spex/spex-category-delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

}
