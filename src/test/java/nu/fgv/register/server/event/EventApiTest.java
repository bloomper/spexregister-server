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

package nu.fgv.register.server.event;

import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = EventApi.class)
class EventApiTest extends AbstractApiTest {

    private final ResponseFieldsSnippet responseFields = responseFields(
            fieldWithPath("id").description("The id of the event"),
            fieldWithPath("event").description("The type of the event"),
            fieldWithPath("source").description("The source of the event"),
            fieldWithPath("createdBy").description("Who created the entity"),
            fieldWithPath("createdAt").description("When was the entity created"),
            linksSubsection
    );
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("news-events").description("Link to news events").optional(),
            linkWithRel("spex-events").description("Link to spex events").optional(),
            linkWithRel("spex-category-events").description("Link to spex category events").optional(),
            linkWithRel("spexare-events").description("Link to spexare events").optional(),
            linkWithRel("tag-events").description("Link to tag events").optional(),
            linkWithRel("task-events").description("Link to task events").optional(),
            linkWithRel("task-category-events").description("Link to task category events").optional(),
            linkWithRel("user-events").description("Link to user events").optional()
    );
    @MockitoBean
    private EventService service;

    @Test
    void should_get_all() throws Exception {
        final var event1 = EventDto.builder().id(1L).eventType(Event.EventType.CREATE.name()).sourceType(Event.SourceType.SPEX.name()).build();
        final var event2 = EventDto.builder().id(2L).eventType(Event.EventType.UPDATE.name()).sourceType(Event.SourceType.TASK.name()).build();

        when(service.findBySourceType(any(Event.SourceType.class), any(Integer.class))).thenReturn(List.of(event1, event2));

        mockMvc
                .perform(
                        get("/api/events?sourceType=NEWS&sinceInDays=30")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "event-get-all",
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
                                queryParameters(
                                        parameterWithName("sourceType").description("The source type of the event"),
                                        parameterWithName("sinceInDays").description("How many days back to check for events")
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(EventApi.class, "retrieve", Event.SourceType.class, Integer.class))
                        )
                );
    }

}
