/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.news;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ExportType;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Locale;

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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = NewsApi.class)
class NewsApiTest extends AbstractApiTest {

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
    @MockitoBean
    private NewsService service;
    @MockitoBean
    private NewsImportService importService;
    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private JobService jobService;
    @MockitoBean
    private EventApi eventApi;

    @Test
    void should_get_paged() throws Exception {
        final var news1 = NewsDto.builder().id(1L).subject("News 1 subject").text("News 1 text").build();
        final var news2 = NewsDto.builder().id(2L).subject("News 2 subject").text("News 2 text").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(news1, news2), PageRequest.of(1, 2, Sort.by("visibleFrom")), 10));

        mockMvc
                .perform(
                        get("/api/news?page=1&size=2&sort=visibleFrom,desc&filter=subject~test")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.news", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "news-get-all-paged",
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
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(NewsApi.class, "retrieve", Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        when(jobService.createExportJob(any(Class.class), anyList(), any(String.class), any(ExportType.class), any(Locale.class))).thenReturn(1L);

        mockMvc
                .perform(
                        get("/api/news?ids=1,2,3&type=excel")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "news-get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("type").description("The export type (excel, excel_xls and pdf supported)"),
                                        parameterWithName("ids").description("The ids of the news to export").optional(),
                                        parameterWithName("filter").description("The filter to use for the news to export").optional()
                                ),
                                exportResponseFields,
                                security(getRolesFromMethod(NewsApi.class, "retrieve", List.class, String.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(NewsCreateDto.class);
        final var dto = NewsCreateDto.builder().subject("News subject").text("News text").build();

        when(service.create(any(NewsCreateDto.class))).thenReturn(NewsDto.builder().id(1L).subject(dto.subject()).text(dto.text()).build());

        mockMvc
                .perform(
                        post("/api/news")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "news-create",
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
                                createResponseHeaders,
                                security(getRolesFromMethod(NewsApi.class, "create", NewsCreateDto.class))
                        )
                );
    }

    @Test
    void should_create_import() throws Exception {
        final var importResult = ImportResultDto.builder().success(true).build();

        when(importService.doImport(any(), any(String.class), any(Locale.class))).thenReturn(importResult);

        mockMvc
                .perform(
                        post("/api/news")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(Constants.MediaTypes.APPLICATION_XLSX)
                                .content(new byte[]{1, 2, 3})
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(
                        document(
                                "news-create-import",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet and application/vnd.ms-excel supported)")
                                ),
                                importResponseFields,
                                security(getRolesFromMethod(NewsApi.class, "createAndUpdate", byte[].class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.findById(any(Long.class))).thenReturn(news);

        mockMvc
                .perform(
                        get("/api/news/{id}", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "news-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the news")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(NewsApi.class, "retrieve", Long.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(NewsUpdateDto.class);
        final var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();
        final var dto = NewsUpdateDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.update(any(NewsUpdateDto.class))).thenReturn(news);

        mockMvc
                .perform(
                        put("/api/news/{id}", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "news-update",
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
                                responseHeaders,
                                security(getRolesFromMethod(NewsApi.class, "update", Long.class, NewsUpdateDto.class))
                        )
                );
    }

    @Test
    void should_partial_update() throws Exception {
        final var fields = new ConstrainedFields(NewsUpdateDto.class);
        final var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();
        final var dto = NewsUpdateDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.partialUpdate(any(NewsUpdateDto.class))).thenReturn(news);

        mockMvc
                .perform(
                        patch("/api/news/{id}", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "news-update-partial",
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
                                responseHeaders,
                                security(getRolesFromMethod(NewsApi.class, "partialUpdate", Long.class, NewsUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        final var news = NewsDto.builder().id(1L).subject("News subject").text("News text").build();

        when(service.findById(any(Long.class))).thenReturn(news);
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/news/{id}", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "news-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the news")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(NewsApi.class, "delete", Long.class))
                        )
                );
    }

    @Test
    void should_get_events() throws Exception {
        final var event1 = EventDto.builder().id(1L).eventType(Event.EventType.CREATE.name()).sourceType(Event.SourceType.NEWS.name()).build();
        final var event2 = EventDto.builder().id(2L).eventType(Event.EventType.UPDATE.name()).sourceType(Event.SourceType.NEWS.name()).build();
        final var realEventApi = new EventApi(null);

        when(eventService.findBySourceTypeAndId(any(Event.SourceType.class), any(Long.class), any(Integer.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/news/events/{sourceId}?sinceInDays=30", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "news-event-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.events[]").description("The elements"),
                                        fieldWithPath("_embedded.events[].id").description("The id of the event"),
                                        fieldWithPath("_embedded.events[].eventType").description("The type of the event"),
                                        fieldWithPath("_embedded.events[].sourceType").description("The source type of the event"),
                                        fieldWithPath("_embedded.events[].sourceId").description("The source id of the event"),
                                        fieldWithPath("_embedded.events[].createdBy").description("Who created the event"),
                                        fieldWithPath("_embedded.events[].createdAt").description("When was the event created"),
                                        subsectionWithPath("_embedded.events[]._links").description("The event links"),
                                        linksSubsection
                                ),
                                pathParameters(
                                        parameterWithName("sourceId").description("The source id of the event")
                                ),
                                queryParameters(
                                        parameterWithName("sinceInDays").description("How many days back to check for events")
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(NewsApi.class, "retrieveEvents", Long.class, Integer.class))
                        )
                );
    }

}
