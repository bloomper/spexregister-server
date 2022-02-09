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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SpexApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SpexApiTest extends AbstractApiTest {

    @MockBean
    private SpexService service;

    @MockBean
    private SpexCategoryApi spexCategoryApi;

    @Test
    public void should_get_spex() throws Exception {
        var spex1 = SpexDto.builder().id(1L).year("2021").build();
        var spex2 = SpexDto.builder().id(1L).year("2022").build();
        when(service.find(eq(false), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spex1, spex2), PageRequest.of(1, 2, Sort.by("year")), 10));
        this.mockMvc.perform(get("/api/v1/spex?page=1&size=2&sort=year,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spex",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), removeHeaders(HttpHeaders.CONTENT_LENGTH)),
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
                                        fieldWithPath("_embedded.spex[].createdDate").description("When was the spex"),
                                        fieldWithPath("_embedded.spex[].lastModifiedBy").description("Who last modified the spex"),
                                        fieldWithPath("_embedded.spex[].lastModifiedDate").description("When was the spex last modified"),
                                        subsectionWithPath("_embedded.spex[]._links").description("The spex links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingRequestParameters,
                                responseHeaders
                        )
                );
    }
}
