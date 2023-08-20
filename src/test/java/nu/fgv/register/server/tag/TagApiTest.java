package nu.fgv.register.server.tag;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TagApi.class)
public class TagApiTest extends AbstractApiTest {

    @MockBean
    private TagService service;

    @MockBean
    private TagImportService importService;

    @MockBean
    private TagExportService exportService;

    @MockBean
    private EventService eventService;

    @MockBean
    private EventApi eventApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the tag"),
            fieldWithPath("name").description("The name of the tag"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("tags").description("Link to paged tags").optional(),
            linkWithRel("events").description("Link to tag events").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var tag1 = TagDto.builder().id(1L).name("tag1").build();
        var tag2 = TagDto.builder().id(2L).name("tag2").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(tag1, tag2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/v1/tags?page=1&size=2&sort=name,asc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.tags", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "tags/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
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
                        get("/api/v1/tags?ids=1,2,3")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "tags/get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the tags to export").optional()
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
        var fields = new ConstrainedFields(TagCreateDto.class);
        var dto = TagCreateDto.builder().name("Tag").build();

        when(service.create(any(TagCreateDto.class))).thenReturn(TagDto.builder().id(1L).name(dto.getName()).build());

        mockMvc
                .perform(
                        post("/api/v1/tags")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "tags/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the tag")
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
        var tag = TagDto.builder().id(1L).name("tag").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(tag));

        mockMvc
                .perform(
                        get("/api/v1/tags/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tags/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
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
        var fields = new ConstrainedFields(TagUpdateDto.class);
        var tag = TagDto.builder().id(1L).name("tag").build();
        var dto = TagUpdateDto.builder().id(1L).name("tag2").build();

        when(service.update(any(TagUpdateDto.class))).thenReturn(Optional.of(tag));

        mockMvc
                .perform(
                        put("/api/v1/tags/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tags/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the tag"),
                                        fields.withPath("name").description("The name of the tag")
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
        var fields = new ConstrainedFields(TagUpdateDto.class);
        var tag = TagDto.builder().id(1L).name("tag").build();
        var dto = TagUpdateDto.builder().id(1L).build();

        when(service.partialUpdate(any(TagUpdateDto.class))).thenReturn(Optional.of(tag));

        mockMvc
                .perform(
                        patch("/api/v1/tags/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tags/partial-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the tag"),
                                        fields.withPath("name").description("The name of the tag").optional()
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
        var tag = TagDto.builder().id(1L).name("tag").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(tag));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/tags/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "tags/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the tag")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_get_events() throws Exception {
        var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.TAG.name()).build();
        var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.TAG.name()).build();
        var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/tags/events?sinceInDays=30")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "tags/get-events",
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
