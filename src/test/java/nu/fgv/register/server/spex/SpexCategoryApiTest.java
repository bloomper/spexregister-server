package nu.fgv.register.server.spex;

import nu.fgv.register.server.util.AbstractApiTest;
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
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
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

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex category"),
            fieldWithPath("name").description("The name of the spex category"),
            fieldWithPath("firstYear").description("The first year of the spex category"),
            fieldWithPath("logo").description("The logo of the spex category").optional(),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("logo").description("Link to the current spex category's logo").optional()
    );

    @Test
    public void should_get_paged_spex_categories() throws Exception {
        var category1 = SpexCategoryDto.builder().id(1L).name("category1").logo("logo1").build();
        var category2 = SpexCategoryDto.builder().id(2L).name("category2").logo("logo1").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category1, category2), PageRequest.of(1, 2, Sort.by("name")), 10));

        this.mockMvc
                .perform(
                        get("/api/v1/spex-categories?page=1&size=2&sort=name,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spexCategories", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spexCategories[]").description("The elements"),
                                        fieldWithPath("_embedded.spexCategories[].id").description("The id of the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].name").description("The name of the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].firstYear").description("The first year of the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].logo").description("The logo of the spex category").optional(),
                                        fieldWithPath("_embedded.spexCategories[].createdBy").description("Who created the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].createdDate").description("When was the spex category created"),
                                        fieldWithPath("_embedded.spexCategories[].lastModifiedBy").description("Who last modified the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].lastModifiedDate").description("When was the spex category last modified"),
                                        subsectionWithPath("_embedded.spexCategories[]._links").description("The spex category links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingRequestParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_create_spex_category() throws Exception {
        var fields = new ConstrainedFields(SpexCategoryCreateDto.class);
        var dto = SpexCategoryCreateDto.builder().firstYear("1948").name("Chalmersspexet").build();

        when(service.create(any(SpexCategoryCreateDto.class))).thenReturn(SpexCategoryDto.builder().id(1L).firstYear(dto.getFirstYear()).name(dto.getName()).build());

        this.mockMvc
                .perform(
                        post("/api/v1/spex-categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spex-categories/create",
                                preprocessRequest(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH, HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
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
    public void should_get_spex_category() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").logo("logo").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        get("/api/v1/spex-categories/{id}", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
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
    public void should_update_spex_category() throws Exception {
        var fields = new ConstrainedFields(SpexCategoryUpdateDto.class);
        var category = SpexCategoryDto.builder().id(1L).name("category").logo("logo").build();
        var dto = SpexCategoryUpdateDto.builder().id(1L).firstYear("1948").name("Chalmersspexet").build();

        when(service.update(any(SpexCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        put("/api/v1/spex-categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
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
    public void should_partial_update_spex_category() throws Exception {
        var fields = new ConstrainedFields(SpexCategoryUpdateDto.class);
        var category = SpexCategoryDto.builder().id(1L).name("category").logo("logo").build();
        var dto = SpexCategoryUpdateDto.builder().id(1L).firstYear("1948").build();

        when(service.partialUpdate(any(SpexCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        patch("/api/v1/spex-categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/partial-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
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
    public void should_delete_spex_category() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").logo("logo").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));
        doNothing().when(service).deleteById(any(Long.class));

        this.mockMvc
                .perform(
                        delete("/api/v1/spex-categories/{id}", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                )
                        )
                );
    }

    @Test
    public void should_download_spex_category_logo() throws Exception {
        var logo = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);
        when(service.getLogo(any(Long.class))).thenReturn(Optional.of(logo));

        this.mockMvc
                .perform(
                        get("/api/v1/spex-categories/{id}/logo", 1)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, logo.getSecond()))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, logo.getFirst().length))
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/logo-download",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                )
                        )
                );
    }

    @Test
    public void should_upload_spex_category_logo() throws Exception {
        var logo = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        var category = SpexCategoryDto.builder().id(1L).name("category").logo("logo").build();
        when(service.saveLogo(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        multipart("/api/v1/spex-categories/{id}/logo", 1)
                                .file(logo)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/logo-upload",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                ),
                                requestParts(
                                        partWithName("file").description("The logo to upload")
                                )
                        )
                );
    }

    @Test
    public void should_delete_spex_category_logo() throws Exception {
        var category = SpexCategoryDto.builder().id(1L).name("category").logo("logo").build();
        when(service.removeLogo(any(Long.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        delete("/api/v1/spex-categories/{id}/logo", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories/logo-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spex category")
                                )
                        )
                );
    }
}
