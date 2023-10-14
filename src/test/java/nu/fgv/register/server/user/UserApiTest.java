package nu.fgv.register.server.user;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.user.authority.AuthorityApi;
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
import static org.mockito.ArgumentMatchers.anyList;
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

@WebMvcTest(value = UserApi.class)
public class UserApiTest extends AbstractApiTest {

    @MockBean
    private UserService service;

    @MockBean
    private AuthorityApi authorityApi;

    @MockBean
    private EventService eventService;

    @MockBean
    private EventApi eventApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the user"),
            fieldWithPath("username").description("The username of the user"),
            fieldWithPath("state").description("The state of the user"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("users").description("Link to paged users").optional(),
            linkWithRel("authorities").description("Link to user authorities").optional(),
            linkWithRel("events").description("Link to user events").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var user1 = UserDto.builder().id(1L).username("email1@somewhere.com").state(User.State.ACTIVE).build();
        var user2 = UserDto.builder().id(1L).username("email2@somewhere.com").state(User.State.ACTIVE).build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user1, user2), PageRequest.of(1, 2, Sort.by("username")), 10));

        mockMvc
                .perform(
                        get("/api/v1/users?page=1&size=2&sort=username,desc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.users", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "users/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.users[]").description("The elements"),
                                        fieldWithPath("_embedded.users[].id").description("The id of the user"),
                                        fieldWithPath("_embedded.users[].username").description("The username of the user"),
                                        fieldWithPath("_embedded.users[].state").description("The state of the user"),
                                        fieldWithPath("_embedded.users[].createdBy").description("Who created the user"),
                                        fieldWithPath("_embedded.users[].createdAt").description("When was the user created"),
                                        fieldWithPath("_embedded.users[].lastModifiedBy").description("Who last modified the user"),
                                        fieldWithPath("_embedded.users[].lastModifiedAt").description("When was the user last modified"),
                                        subsectionWithPath("_embedded.users[]._links").description("The user links"),
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
        var fields = new ConstrainedFields(UserCreateDto.class);
        var dto = UserCreateDto.builder().username("email@somewhere.com").build();

        when(service.create(any(UserCreateDto.class))).thenReturn(UserDto.builder().id(1L).username(dto.getUsername()).state(User.State.PENDING).build());

        mockMvc
                .perform(
                        post("/api/v1/users")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "users/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("username").description("The username of the user")
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
        var user = UserDto.builder().id(1L).username("email@somewhere.com").state(User.State.ACTIVE).build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(user));

        mockMvc
                .perform(
                        get("/api/v1/users/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "users/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the user")
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
        var fields = new ConstrainedFields(UserUpdateDto.class);
        var user = UserDto.builder().id(1L).username("email@somewhere.com").state(User.State.ACTIVE).build();
        var dto = UserUpdateDto.builder().id(1L).username("email@somewhere.com").build();

        when(service.update(any(UserUpdateDto.class))).thenReturn(Optional.of(user));

        mockMvc
                .perform(
                        put("/api/v1/users/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "users/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the user")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the user"),
                                        fields.withPath("username").description("The username of the user")
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
        var fields = new ConstrainedFields(UserUpdateDto.class);
        var user = UserDto.builder().id(1L).username("email@somewhere.com").state(User.State.ACTIVE).build();
        var dto = UserUpdateDto.builder().id(1L).username("email@somewhere.com").build();

        when(service.partialUpdate(any(UserUpdateDto.class))).thenReturn(Optional.of(user));

        mockMvc
                .perform(
                        patch("/api/v1/users/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "users/partial-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the user")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the user"),
                                        fields.withPath("username").description("The username of the user").optional()
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
        var user = UserDto.builder().id(1L).username("email@somewhere.com").state(User.State.ACTIVE).build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(user));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/users/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "users/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the user")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_add_authority() throws Exception {
        when(service.addAuthority(any(Long.class), any(String.class))).thenReturn(true);

        mockMvc
                .perform(
                        put("/api/v1/users/{userId}/authorities/{id}", 1, "ROLE_USER")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "users/authorities/add",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("userId").description("The id of the user"),
                                        parameterWithName("id").description("The id of the authority")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_add_authorities() throws Exception {
        when(service.addAuthorities(any(Long.class), anyList())).thenReturn(true);

        mockMvc
                .perform(
                        put("/api/v1/users/{userId}/authorities", 1)
                                .queryParam("ids", "ROLE_USER", "ROLE_EDITOR")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "users/authorities/add-multiple",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("userId").description("The id of the user")
                                ),
                                queryParameters(
                                        parameterWithName("ids").description("The ids of the authorities")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_remove_authority() throws Exception {
        when(service.removeAuthority(any(Long.class), any(String.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/users/{userId}/authorities/{id}", 1, "ROLE_USER")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "users/authorities/remove",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("userId").description("The id of the user"),
                                        parameterWithName("id").description("The id of the authority")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_remove_authorities() throws Exception {
        when(service.removeAuthorities(any(Long.class), anyList())).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/users/{userId}/authorities", 1)
                                .queryParam("ids", "ROLE_USER", "ROLE_EDITOR")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "users/authorities/remove-multiple",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("userId").description("The id of the user")
                                ),
                                queryParameters(
                                        parameterWithName("ids").description("The ids of the authorities")
                                ),
                                secureRequestHeaders
                        )
                );
    }

    @Test
    public void should_get_events() throws Exception {
        var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.USER.name()).build();
        var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.USER.name()).build();
        var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/users/events?sinceInDays=30")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "users/get-events",
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
