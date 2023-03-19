package nu.fgv.register.server.task;

import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
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
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TaskApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class TaskApiTest extends AbstractApiTest {

    @MockBean
    private TaskService service;

    @MockBean
    private TaskImportService importService;
    @MockBean
    private TaskExportService exportService;

    @MockBean
    private TaskCategoryApi categoryApi;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task"),
            fieldWithPath("name").description("The name of the task"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("tasks").description("Link to paged tasks").optional(),
            linkWithRel("category").description("Link to the current task's task category").optional()
    );

    private final ResponseFieldsSnippet categoryResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task category"),
            fieldWithPath("name").description("The name of the task category"),
            fieldWithPath("hasActor").description("The flag telling whether the task category can have associated actor information"),
            linksSubsection
    );

    private final LinksSnippet categoryLinks = baseLinks.and(
            linkWithRel("task-categories").description("Link to paged task categories").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var task1 = TaskDto.builder().id(1L).name("Scenmästare").build();
        var task2 = TaskDto.builder().id(2L).name("Ljusmästare").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task1, task2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/v1/tasks?page=1&size=2&sort=name,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.tasks", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/get-paged",
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
                                pagingQueryParameters,
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
                        get("/api/v1/tasks?ids=1,2,3")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the tasks to export").optional()
                                ),
                                requestHeaders(
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
        var fields = new ConstrainedFields(TaskCreateDto.class);
        var dto = TaskCreateDto.builder().name("Scenmästare").build();

        when(service.create(any(TaskCreateDto.class))).thenReturn(TaskDto.builder().id(1L).name(dto.getName()).build());

        mockMvc
                .perform(
                        post("/api/v1/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "tasks/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the task")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var task = TaskDto.builder().id(1L).name("Scenmästare").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(task));

        mockMvc
                .perform(
                        get("/api/v1/tasks/{id}", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        var fields = new ConstrainedFields(TaskUpdateDto.class);
        var task = TaskDto.builder().id(1L).name("Scenmästare").build();
        var dto = TaskUpdateDto.builder().id(1L).name("Scenmästare").build();

        when(service.update(any(TaskUpdateDto.class))).thenReturn(Optional.of(task));

        mockMvc
                .perform(
                        put("/api/v1/tasks/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/update",
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update() throws Exception {
        var fields = new ConstrainedFields(TaskUpdateDto.class);
        var task = TaskDto.builder().id(1L).name("Scenmästare").build();
        var dto = TaskUpdateDto.builder().id(1L).name("Scenmästare").build();

        when(service.partialUpdate(any(TaskUpdateDto.class))).thenReturn(Optional.of(task));

        mockMvc
                .perform(
                        patch("/api/v1/tasks/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/partial-update",
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
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        var task = TaskDto.builder().id(1L).name("Scenmästare").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(task));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/tasks/{id}", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "tasks/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task")
                                )
                        )
                );
    }

    @Test
    public void should_get_category() throws Exception {
        var category = TaskCategoryDto.builder().id(1L).name("category").build();
        var links = List.of(linkTo(methodOn(TaskCategoryApi.class).retrieve(category.getId())).withSelfRel(), linkTo(methodOn(TaskCategoryApi.class).retrieve(Pageable.unpaged())).withRel("task-categories"));

        when(service.findCategoryByTask(any(Long.class))).thenReturn(Optional.of(category));
        when(categoryApi.getLinks(any(TaskCategoryDto.class))).thenReturn(links);

        mockMvc
                .perform(
                        get("/api/v1/tasks/{taskId}/category", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "tasks/category-get",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("taskId").description("The id of the task")
                                ),
                                categoryResponseFields,
                                categoryLinks,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update_category() throws Exception {
        when(service.updateCategory(any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        put("/api/v1/tasks/{taskId}/category/{id}", 1, 1)
                )
                .andExpect(status().isAccepted())
                .andDo(document(
                                "tasks/category-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("taskId").description("The id of the task"),
                                        parameterWithName("id").description("The id of the task category")
                                )
                        )
                );
    }

    @Test
    public void should_delete_category() throws Exception {
        when(service.deleteCategory(any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/tasks/{taskId}/category", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "tasks/category-delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("taskId").description("The id of the task")
                                )
                        )
                );
    }

}
