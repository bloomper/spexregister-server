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

@WebMvcTest(value = SpexCategoryApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SpexCategoryApiTest extends AbstractApiTest {

    @MockBean
    private SpexCategoryService service;

    @MockBean
    private SpexCategoryImportService importService;

    @MockBean
    private SpexCategoryExportService exportService;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex category"),
            fieldWithPath("name").description("The name of the spex category"),
            fieldWithPath("firstYear").description("The first year of the spex category"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spex-categories").description("Link to paged spex categories").optional(),
            linkWithRel("logo").description("Link to the current spex category's logo").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var category1 = SpexCategoryDto.builder().id(1L).name("category1").build();
        var category2 = SpexCategoryDto.builder().id(2L).name("category2").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category1, category2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spex/categories?page=1&size=2&sort=name,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex-categories", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/get-paged",
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
                                pagingQueryParameters,
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
                        get("/api/v1/spex/categories?ids=1,2,3")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the spex categories to export").optional()
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
    public void should_create() throws Exception {
        var fields = new ConstrainedFields(SpexCategoryCreateDto.class);
        var dto = SpexCategoryCreateDto.builder().firstYear("1948").name("Chalmersspexet").build();

        when(service.create(any(SpexCategoryCreateDto.class))).thenReturn(SpexCategoryDto.builder().id(1L).firstYear(dto.getFirstYear()).name(dto.getName()).build());

        mockMvc
                .perform(
                        post("/api/v1/spex/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex/categories/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the spex category"),
                                        fields.withPath("firstYear").description("The first year of the spex category")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        get("/api/v1/spex/categories/{id}", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        var fields = new ConstrainedFields(SpexCategoryUpdateDto.class);
        var category = SpexCategoryDto.builder().id(1L).name("category").build();
        var dto = SpexCategoryUpdateDto.builder().id(1L).firstYear("1948").name("Chalmersspexet").build();

        when(service.update(any(SpexCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        put("/api/v1/spex/categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/update",
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update() throws Exception {
        var fields = new ConstrainedFields(SpexCategoryUpdateDto.class);
        var category = SpexCategoryDto.builder().id(1L).name("category").build();
        var dto = SpexCategoryUpdateDto.builder().id(1L).firstYear("1948").build();

        when(service.partialUpdate(any(SpexCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        patch("/api/v1/spex/categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/partial-update",
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/spex/categories/{id}", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                )
                        )
                );
    }

    @Test
    public void should_download_logo() throws Exception {
        var logo = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);
        when(service.getLogo(any(Long.class))).thenReturn(Optional.of(logo));

        mockMvc
                .perform(
                        get("/api/v1/spex/categories/{spexId}/logo", 1)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, logo.getSecond()))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, logo.getFirst().length))
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/logo-download",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
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
    public void should_upload_logo() throws Exception {
        var logo = new byte[]{10, 12};
        var category = SpexCategoryDto.builder().id(1L).name("category").build();
        when(service.saveLogo(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        put("/api/v1/spex/categories/{spexId}/logo", 1)
                                .contentType(MediaType.IMAGE_PNG)
                                .content(logo)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/logo-upload",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
                                ),
                                requestHeaders(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (image/png, image/jpeg and image/gif supported)")
                                ),
                                requestBody()
                        )
                );
    }

    @Test
    public void should_upload_logo_via_multipart() throws Exception {
        var logo = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        var category = SpexCategoryDto.builder().id(1L).name("category").build();
        when(service.saveLogo(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        multipart("/api/v1/spex/categories/{spexId}/logo", 1)
                                .file(logo)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/logo-upload-multipart",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
                                ),
                                requestParts(
                                        partWithName("file").description("The logo to upload")
                                )
                        )
                );
    }

    @Test
    public void should_delete_logo() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").build();
        when(service.deleteLogo(any(Long.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        delete("/api/v1/spex/categories/{spexId}/logo", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex/categories/logo-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexId").description("The id of the spex category")
                                )
                        )
                );
    }
}
