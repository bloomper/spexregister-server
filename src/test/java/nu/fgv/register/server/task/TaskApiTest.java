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

package nu.fgv.register.server.task;

import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.task.category.TaskCategoryApi;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
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

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = TaskApi.class)
class TaskApiTest extends AbstractApiTest {

    @MockitoBean
    private TaskService service;

    @MockitoBean
    private TaskImportService importService;

    @MockitoBean
    private TaskExportService exportService;

    @MockitoBean
    private TaskCategoryApi categoryApi;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventApi eventApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task"),
            fieldWithPath("name").description("The name of the task"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("tasks").description("Link to paged tasks").optional(),
            linkWithRel("category").description("Link to the current task's task category").optional(),
            linkWithRel("events").description("Link to task events").optional()
    );

    private final ResponseFieldsSnippet categoryResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task category"),
            fieldWithPath("name").description("The name of the task category"),
            fieldWithPath("hasActor").description("The flag telling whether the task category can have associated actor information"),
            linksSubsection
    );

    private final LinksSnippet categoryLinks = baseLinks.and(
            linkWithRel("task-categories").description("Link to paged task categories").optional(),
            linkWithRel("events").description("Link to task category events").optional()
    );

    @Test
    void should_get_paged() throws Exception {
        final var task1 = TaskDto.builder().id(1L).name("Scenmästare").build();
        final var task2 = TaskDto.builder().id(2L).name("Ljusmästare").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task1, task2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/v1/tasks?page=1&size=2&sort=name,desc&filter=name:whatever")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.tasks", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "task-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.tasks[]").description("The elements"),
                                        fieldWithPath("_embedded.tasks[].id").description("The id of the task"),
                                        fieldWithPath("_embedded.tasks[].name").description("The name of the task"),
                                        fieldWithPath("_embedded.tasks[].createdBy").description("Who created the task"),
                                        fieldWithPath("_embedded.tasks[].createdAt").description("When was the task created"),
                                        fieldWithPath("_embedded.tasks[].lastModifiedBy").description("Who last modified the task"),
                                        fieldWithPath("_embedded.tasks[].lastModifiedAt").description("When was the task last modified"),
                                        subsectionWithPath("_embedded.tasks[]._links").description("The task links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TaskApi.class, "retrieve", Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        final var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        mockMvc
                .perform(
                        get("/api/v1/tasks?ids=1,2,3")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "task-get-all-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the tasks to export").optional()
                                ),
                                secureRequestHeaders.and(
                                        headerWithName(HttpHeaders.ACCEPT).description("The content type (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet and application/vnd.ms-excel supported)")
                                ),
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody(),
                                security(getRolesFromMethod(TaskApi.class, "retrieve", List.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(TaskCreateDto.class);
        final var dto = TaskCreateDto.builder().name("Scenmästare").build();

        when(service.create(any(TaskCreateDto.class))).thenReturn(TaskDto.builder().id(1L).name(dto.getName()).build());

        mockMvc
                .perform(
                        post("/api/v1/tasks")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "task-create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the task")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders,
                                security(getRolesFromMethod(TaskApi.class, "create", TaskCreateDto.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var task = TaskDto.builder().id(1L).name("Scenmästare").build();

        when(service.findById(any(Long.class))).thenReturn(task);

        mockMvc
                .perform(
                        get("/api/v1/tasks/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "task-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TaskApi.class, "retrieve", Long.class))
                        )
                );
    }

    @Test
    void should_update() throws Exception {
        final var fields = new ConstrainedFields(TaskUpdateDto.class);
        final var task = TaskDto.builder().id(1L).name("Scenmästare").build();
        final var dto = TaskUpdateDto.builder().id(1L).name("Scenmästare").build();

        when(service.update(any(TaskUpdateDto.class))).thenReturn(task);

        mockMvc
                .perform(
                        put("/api/v1/tasks/{id}", 1L)
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
                                "task-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the task"),
                                        fields.withPath("name").description("The name of the task")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TaskApi.class, "update", Long.class, TaskUpdateDto.class))
                        )
                );
    }

    @Test
    void should_partial_update() throws Exception {
        final var fields = new ConstrainedFields(TaskUpdateDto.class);
        final var task = TaskDto.builder().id(1L).name("Scenmästare").build();
        final var dto = TaskUpdateDto.builder().id(1L).name("Scenmästare").build();

        when(service.partialUpdate(any(TaskUpdateDto.class))).thenReturn(task);

        mockMvc
                .perform(
                        patch("/api/v1/tasks/{id}", 1L)
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
                                "task-update-partial",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the task"),
                                        fields.withPath("name").description("The name of the task").optional()
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TaskApi.class, "partialUpdate", Long.class, TaskUpdateDto.class))
                        )
                );
    }

    @Test
    void should_delete() throws Exception {
        final var task = TaskDto.builder().id(1L).name("Scenmästare").build();

        when(service.findById(any(Long.class))).thenReturn(task);
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/tasks/{id}", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "task-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(TaskApi.class, "delete", Long.class))
                        )
                );
    }

    @Test
    void should_get_category() throws Exception {
        final var category = TaskCategoryDto.builder().id(1L).name("category").build();
        final var realCategoryApi = new TaskCategoryApi(null, null, null, null, null, null);

        when(service.findCategoryByTask(any(Long.class))).thenReturn(category);
        when(categoryApi.getLinks(any(TaskCategoryDto.class))).thenReturn(realCategoryApi.getLinks(category));

        mockMvc
                .perform(
                        get("/api/v1/tasks/{taskId}/category", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "task-category-get",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("taskId").description("The id of the task")
                                ),
                                categoryResponseFields,
                                categoryLinks,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TaskApi.class, "retrieveCategory", Long.class))
                        )
                );
    }

    @Test
    void should_add_category() throws Exception {
        mockMvc
                .perform(
                        put("/api/v1/tasks/{taskId}/category/{id}", 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "task-category-add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("taskId").description("The id of the task"),
                                        parameterWithName("id").description("The id of the task category")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(TaskApi.class, "addCategory", Long.class, Long.class))
                        )
                );
    }

    @Test
    void should_remove_category() throws Exception {
        mockMvc
                .perform(
                        delete("/api/v1/tasks/{taskId}/category", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "task-category-remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("taskId").description("The id of the task")
                                ),
                                secureRequestHeaders,
                                security(getRolesFromMethod(TaskApi.class, "removeCategory", Long.class))
                        )
                );
    }

    @Test
    void should_get_events() throws Exception {
        final var event1 = EventDto.builder().id(1L).event(Event.EventType.CREATE.name()).source(Event.SourceType.TASK.name()).build();
        final var event2 = EventDto.builder().id(2L).event(Event.EventType.UPDATE.name()).source(Event.SourceType.TASK.name()).build();
        final var realEventApi = new EventApi(null);

        when(eventService.findBySource(any(Integer.class), any(Event.SourceType.class))).thenReturn(List.of(event1, event2));
        when(eventApi.getLinks(event1)).thenReturn(realEventApi.getLinks(event1));
        when(eventApi.getLinks(event2)).thenReturn(realEventApi.getLinks(event2));

        mockMvc
                .perform(
                        get("/api/v1/tasks/events?sinceInDays=30")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.events", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "task-event-get",
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
                                responseHeaders,
                                security(getRolesFromMethod(TaskApi.class, "retrieveEvents", Integer.class))
                        )
                );
    }

}
