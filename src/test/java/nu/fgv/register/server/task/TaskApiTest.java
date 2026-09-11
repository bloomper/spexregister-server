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

import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.task.category.TaskCategoryApi;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.util.AbstractApiTest;
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
@WebMvcTest(value = TaskApi.class)
class TaskApiTest extends AbstractApiTest {

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task"),
            fieldWithPath("name").description("The name of the task"),
            linksSubsection
    );
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("tasks").description("Link to paged tasks").optional(),
            linkWithRel("category").description("Link to the current task's task category").optional(),
            linkWithRel("revisions").description("Link to revisions").optional()
    );
    private final ResponseFieldsSnippet categoryResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task category"),
            fieldWithPath("name").description("The name of the task category"),
            fieldWithPath("actorPresent").description("The flag telling whether the task category can have associated actor information"),
            linksSubsection
    );
    private final LinksSnippet categoryLinks = baseLinks.and(
            linkWithRel("task-categories").description("Link to paged task categories").optional(),
            linkWithRel("revisions").description("Link to revisions").optional()
    );
    @MockitoBean
    private TaskService service;
    @MockitoBean
    private TaskImportService importService;
    @MockitoBean
    private TaskCategoryApi categoryApi;
    @MockitoBean
    private JobService jobService;

    @Test
    void should_get_paged() throws Exception {
        final var task1 = TaskDto.builder().id(1L).name("Scenmästare").build();
        final var task2 = TaskDto.builder().id(2L).name("Ljusmästare").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task1, task2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/tasks?page=1&size=2&sort=name,desc&filter=name:whatever")
                                .apiVersion("1.0")
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
        when(jobService.createExportJob(any(Class.class), anyList(), any(String.class), any(ImpexType.class), any(Locale.class))).thenReturn(1L);

        mockMvc
                .perform(
                        get("/api/tasks?ids=1,2,3&type=excel")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "task-get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("type").description("The impex type (excel, excel_xls and pdf supported)"),
                                        parameterWithName("ids").description("The ids of the tasks to export").optional(),
                                        parameterWithName("filter").description("The filter to use for the tasks to export").optional()
                                ),
                                exportResponseFields,
                                security(getRolesFromMethod(TaskApi.class, "retrieve", List.class, String.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_create() throws Exception {
        final var fields = new ConstrainedFields(TaskCreateDto.class);
        final var dto = TaskCreateDto.builder().name("Scenmästare").build();

        when(service.create(any(TaskCreateDto.class))).thenReturn(TaskDto.builder().id(1L).name(dto.name()).build());

        mockMvc
                .perform(
                        post("/api/tasks")
                                .apiVersion("1.0")
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
    void should_create_import() throws Exception {
        when(jobService.createImportJob(any(), any(), any(ImpexType.class), any(Locale.class))).thenReturn(1L);

        mockMvc
                .perform(
                        post("/api/tasks?type=excel")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .content(new byte[]{1, 2, 3})
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "task-create-import",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("type").description("The impex type (excel, excel_xls and pdf supported)")
                                ),
                                importResponseFields,
                                security(getRolesFromMethod(TaskApi.class, "createAndUpdate", byte[].class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var task = TaskDto.builder().id(1L).name("Scenmästare").build();

        when(service.findById(any(Long.class))).thenReturn(task);

        mockMvc
                .perform(
                        get("/api/tasks/{id}", 1L)
                                .apiVersion("1.0")
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
                        put("/api/tasks/{id}", 1L)
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
                        patch("/api/tasks/{id}", 1L)
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
                        delete("/api/tasks/{id}", 1L)
                                .apiVersion("1.0")
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
        final var realCategoryApi = new TaskCategoryApi(null, null, null);

        when(service.findCategoryByTask(any(Long.class))).thenReturn(category);
        when(categoryApi.getLinks(any(TaskCategoryDto.class))).thenReturn(realCategoryApi.getLinks(category));

        mockMvc
                .perform(
                        get("/api/tasks/{taskId}/category", 1L)
                                .apiVersion("1.0")
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
                        put("/api/tasks/{taskId}/category/{id}", 1L, 1L)
                                .apiVersion("1.0")
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
                        delete("/api/tasks/{taskId}/category", 1L)
                                .apiVersion("1.0")
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

}
