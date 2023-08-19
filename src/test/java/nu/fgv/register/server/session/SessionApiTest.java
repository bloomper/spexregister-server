package nu.fgv.register.server.session;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

@WebMvcTest(value = SessionApi.class)
public class SessionApiTest extends AbstractApiTest {

    @MockBean
    private EventService eventService;

    @MockBean
    private EventApi eventApi;

    @Test
    public void should_get_events() throws Exception {
        var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.SESSION.name()).build();
        var event2 = EventDto.builder().id(2L).event(Event.EventType.CREATE.name()).source(Event.SourceType.SESSION.name()).build();
        var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/sessions/events?sinceInDays=30")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "sessions/get-events",
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
                                responseHeaders
                        )
                );
    }

}
