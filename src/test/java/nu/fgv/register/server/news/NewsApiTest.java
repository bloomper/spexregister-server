package nu.fgv.register.server.news;

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
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = NewsApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class NewsApiTest extends AbstractApiTest {

    @MockBean
    private NewsService service;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the news"),
            fieldWithPath("subject").description("The subject of the news"),
            fieldWithPath("text").description("The text of the news"),
            fieldWithPath("visibleFrom").description("The visible from of the news"),
            fieldWithPath("visibleTo").description("The visible to of the news"),
            fieldWithPath("published").description("The flag telling whether the news has been published or not"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and();

    @Test
    public void should_get_paged_news() throws Exception {
        var news1 = NewsDto.builder().id(1L).subject("News 1 subject").text("News 1 text").build();
        var news2 = NewsDto.builder().id(2L).subject("News 2 subject").text("News 2 text").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(news1, news2), PageRequest.of(1, 2, Sort.by("visibleFrom")), 10));

        mockMvc
                .perform(
                        get("/api/v1/news?page=1&size=2&sort=visibleFrom,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.news", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "news/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.news[]").description("The elements"),
                                        fieldWithPath("_embedded.news[].id").description("The id of the news"),
                                        fieldWithPath("_embedded.news[].subject").description("The subject of the news"),
                                        fieldWithPath("_embedded.news[].text").description("The text of the news"),
                                        fieldWithPath("_embedded.news[].visibleFrom").description("The visible from of the news"),
                                        fieldWithPath("_embedded.news[].visibleTo").description("The visible to of the news"),
                                        fieldWithPath("_embedded.news[].createdBy").description("Who created the news"),
                                        fieldWithPath("_embedded.news[].createdAt").description("When was the news created"),
                                        fieldWithPath("_embedded.news[].lastModifiedBy").description("Who last modified the news"),
                                        fieldWithPath("_embedded.news[].lastModifiedAt").description("When was the news last modified"),
                                        subsectionWithPath("_embedded.news[]._links").description("The news links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_create_news() throws Exception {
        var fields = new ConstrainedFields(NewsCreateDto.class);
        var dto = NewsCreateDto.builder().subject("News subject").text("News text").build();

        when(service.create(any(NewsCreateDto.class))).thenReturn(NewsDto.builder().id(1L).subject(dto.getSubject()).text(dto.getText()).build());

        mockMvc
                .perform(
                        post("/api/v1/news")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "news/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("subject").description("The subject of the news"),
                                        fields.withPath("text").description("The text of the news"),
                                        fields.withPath("visibleFrom").description("The visible from of the news"),
                                        fields.withPath("visibleTo").description("The visible to of the news")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_news() throws Exception {
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(news));

        mockMvc
                .perform(
                        get("/api/v1/news/{id}", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "news/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the news")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update_news() throws Exception {
        var fields = new ConstrainedFields(NewsUpdateDto.class);
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();
        var dto = NewsUpdateDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.update(any(NewsUpdateDto.class))).thenReturn(Optional.of(news));

        mockMvc
                .perform(
                        put("/api/v1/news/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "news/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the news")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the news"),
                                        fields.withPath("subject").description("The subject of the news"),
                                        fields.withPath("text").description("The text of the news"),
                                        fields.withPath("visibleFrom").description("The visible from of the news"),
                                        fields.withPath("visibleTo").description("The visible to of the news")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update_news() throws Exception {
        var fields = new ConstrainedFields(NewsUpdateDto.class);
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();
        var dto = NewsUpdateDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.partialUpdate(any(NewsUpdateDto.class))).thenReturn(Optional.of(news));

        mockMvc
                .perform(
                        patch("/api/v1/news/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "news/partial-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the news")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the news"),
                                        fields.withPath("subject").description("The subject of the news").optional(),
                                        fields.withPath("text").description("The text of the news").optional(),
                                        fields.withPath("visibleFrom").description("The visible from of the news").optional(),
                                        fields.withPath("visibleTo").description("The visible to of the news").optional()
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete_news() throws Exception {
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(news));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/news/{id}", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "news/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the news")
                                )
                        )
                );
    }

}
