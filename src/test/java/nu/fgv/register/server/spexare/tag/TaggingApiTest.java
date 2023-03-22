package nu.fgv.register.server.spexare.tag;

import nu.fgv.register.server.tag.TagDto;
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
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TaggingApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class TaggingApiTest extends AbstractApiTest {

    @MockBean
    private TaggingService service;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the tag"),
            fieldWithPath("name").description("The name of the tag"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("tags").description("Link to the current spexare's tags")
    );

    @Test
    public void should_get_paged() throws Exception {
        var tag1 = TagDto.builder().id(1L).name("tag1").build();
        var tag2 = TagDto.builder().id(2L).name("tag2").build();

        when(service.findBySpexare(any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(tag1, tag2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/tags?page=1&size=2&sort=name,desc", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.tags", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/tags/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.tags[]").description("The elements"),
                                        fieldWithPath("_embedded.tags[].id").description("The id of the tag"),
                                        fieldWithPath("_embedded.tags[].name").description("The name of the tag"),
                                        fieldWithPath("_embedded.tags[].createdBy").description("Who created the tag"),
                                        fieldWithPath("_embedded.tags[].createdAt").description("When was the tag created"),
                                        fieldWithPath("_embedded.tags[].lastModifiedBy").description("Who last modified the tag"),
                                        fieldWithPath("_embedded.tags[].lastModifiedAt").description("When was the tag last modified"),
                                        subsectionWithPath("_embedded.tags[]._links").description("The tag links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_create() throws Exception {
        var tag = TagDto.builder().id(1L).name("tag").build();

        when(service.create(any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        post("/api/v1/spexare/{spexareId}/tags/{id}", 1, tag.getId())
                )
                .andExpect(status().isCreated())
                .andDo(document(
                                "spexare/tags/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the tag")
                                )
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        when(service.deleteById(any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/tags/{id}", 1, 1)
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare/tags/delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the tag")
                                )
                        )
                );
    }

}
