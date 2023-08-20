package nu.fgv.register.server.news;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = NewsApi.class)
public class NewsApiTest extends AbstractApiTest {

    @MockBean
    private NewsService service;

    @MockBean
    private EventService eventService;

    @MockBean
    private EventApi eventApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the news"),
            fieldWithPath("subject").description("The subject of the news"),
            fieldWithPath("text").description("The text of the news"),
            fieldWithPath("visibleFrom").description("The visible from of the news"),
            fieldWithPath("visibleTo").description("The visible to of the news"),
            fieldWithPath("published").description("The flag telling whether the news has been published or not"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("news").description("Link to paged news").optional(),
            linkWithRel("events").description("Link to news events").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var news1 = NewsDto.builder().id(1L).subject("News 1 subject").text("News 1 text").build();
        var news2 = NewsDto.builder().id(2L).subject("News 2 subject").text("News 2 text").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(news1, news2), PageRequest.of(1, 2, Sort.by("visibleFrom")), 10));

        mockMvc
                .perform(
                        get("/api/v1/news?page=1&size=2&sort=visibleFrom,desc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
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
                                        fieldWithPath("_embedded.news[].published").description("The flag telling whether the news has been published or not"),
                                        fieldWithPath("_embedded.news[].createdBy").description("Who created the news"),
                                        fieldWithPath("_embedded.news[].createdAt").description("When was the news created"),
                                        fieldWithPath("_embedded.news[].lastModifiedBy").description("Who last modified the news"),
                                        fieldWithPath("_embedded.news[].lastModifiedAt").description("When was the news last modified"),
                                        subsectionWithPath("_embedded.news[]._links").description("The news links"),
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
    public void should_create() throws Exception {
        var fields = new ConstrainedFields(NewsCreateDto.class);
        var dto = NewsCreateDto.builder().subject("News subject").text("News text").build();

        when(service.create(any(NewsCreateDto.class))).thenReturn(NewsDto.builder().id(1L).subject(dto.getSubject()).text(dto.getText()).build());

        mockMvc
                .perform(
                        post("/api/v1/news")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
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
                                secureRequestHeaders,
                                createResponseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(news));

        mockMvc
                .perform(
                        get("/api/v1/news/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
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
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        var fields = new ConstrainedFields(NewsUpdateDto.class);
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();
        var dto = NewsUpdateDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.update(any(NewsUpdateDto.class))).thenReturn(Optional.of(news));

        mockMvc
                .perform(
                        put("/api/v1/news/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
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
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update() throws Exception {
        var fields = new ConstrainedFields(NewsUpdateDto.class);
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();
        var dto = NewsUpdateDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.partialUpdate(any(NewsUpdateDto.class))).thenReturn(Optional.of(news));

        mockMvc
                .perform(
                        patch("/api/v1/news/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
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
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(news));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/news/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
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
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_get_events() throws Exception {
        var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.NEWS.name()).build();
        var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.NEWS.name()).build();
        var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/news/events?sinceInDays=30")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "news/get-events",
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
