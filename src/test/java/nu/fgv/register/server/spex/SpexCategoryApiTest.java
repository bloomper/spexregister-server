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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SpexCategoryApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SpexCategoryApiTest extends AbstractApiTest {

    @MockBean
    private SpexCategoryService service;

    @Test
    public void should_get_spex_categories() throws Exception {
        var category1 = SpexCategoryDto.builder().id(1L).name("category1").logo("logo1").build();
        var category2 = SpexCategoryDto.builder().id(2L).name("category2").logo("logo1").build();
        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category1, category2), PageRequest.of(1, 2, Sort.by("name")), 10));
        this.mockMvc.perform(
                        get("/api/v1/spex-categories?page=1&size=2&sort=name,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spexCategories", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex-categories-get-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spexCategories[]").description("The elements"),
                                        fieldWithPath("_embedded.spexCategories[].id").description("The id of the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].name").description("The name of the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].firstYear").description("The first year of the spex category"),
                                        fieldWithPath("_embedded.spexCategories[].logo").description("The logo of the spex category"),
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
        final ConstrainedFields fields = new ConstrainedFields(SpexCategoryDto.class);
        final SpexCategoryCreateDto dto = SpexCategoryCreateDto.builder().firstYear("1948").name("Chalmersspexet").build();

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
                                "spex-categories-create",
                                preprocessRequest(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH, HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the spex category"),
                                        fields.withPath("firstYear").description("The first year of the spex category")
                                ),
                                responseFields(
                                        fieldWithPath("id").description("The id of the spex category"),
                                        fieldWithPath("name").description("The name of the spex category"),
                                        fieldWithPath("firstYear").description("The first year of the spex category"),
                                        fieldWithPath("logo").description("The logo of the spex category"),
                                        fieldWithPath("createdBy").description("Who created the spex category"),
                                        fieldWithPath("createdDate").description("When was the spex category created"),
                                        fieldWithPath("lastModifiedBy").description("Who last modified the spex category"),
                                        fieldWithPath("lastModifiedDate").description("When was the spex category last modified"),
                                        linksSubsection
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("Link to the current entity")
                                ),
                                responseHeaders
                        )
                );
    }
}
