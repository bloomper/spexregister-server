package nu.fgv.register.server.spexare.activity.task;

import nu.fgv.register.server.task.TaskApi;
import nu.fgv.register.server.task.TaskDto;
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
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TaskActivityApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class TaskActivityApiTest extends AbstractApiTest {

    @MockBean
    private TaskActivityService service;

    @MockBean
    private TaskApi taskApi;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task activity"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("activities").description("Link to the current spexare's activities"),
            linkWithRel("task-activities").description("Link to the current spexare's task activities"),
            linkWithRel("task").description("Link to the current task")
    );

    private final ResponseFieldsSnippet taskResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task"),
            fieldWithPath("name").description("The name of the task"),
            linksSubsection
    );

    private final LinksSnippet taskLinks = baseLinks.and(
            linkWithRel("tasks").description("Link to paged tasks").optional(),
            linkWithRel("category").description("Link to the current task's task category").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var taskActivity1 = TaskActivityDto.builder().id(1L).build();
        var taskActivity2 = TaskActivityDto.builder().id(2L).build();

        when(service.findByActivity(any(Long.class), any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(taskActivity1, taskActivity2), PageRequest.of(1, 2, Sort.by("id")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities?page=1&size=2&sort=id,desc", 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.task-activities", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/activities/tasks/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.task-activities[]").description("The elements"),
                                        fieldWithPath("_embedded.task-activities[].id").description("The id of the task activity"),
                                        fieldWithPath("_embedded.task-activities[].createdBy").description("Who created the task activity"),
                                        fieldWithPath("_embedded.task-activities[].createdAt").description("When was the task activity created"),
                                        fieldWithPath("_embedded.task-activities[].lastModifiedBy").description("Who last modified the task activity"),
                                        fieldWithPath("_embedded.task-activities[].lastModifiedAt").description("When was the task activity last modified"),
                                        subsectionWithPath("_embedded.task-activities[]._links").description("The task activity links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var taskActivity = TaskActivityDto.builder().id(1L).build();

        when(service.findById(any(Long.class), any(Long.class), any(Long.class))).thenReturn(Optional.of(taskActivity));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{id}", 1, 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/activities/tasks/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the task activity")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_create() throws Exception {
        var taskActivity = TaskActivityDto.builder().id(1L).build();

        when(service.create(any(Long.class), any(Long.class), any(Long.class))).thenReturn(Optional.of(taskActivity));

        mockMvc
                .perform(
                        post("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskId}", 1, 1, 1)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/activities/tasks/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskId").description("The id of the task")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        when(service.update(any(Long.class), any(Long.class), any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{id}/{taskId}", 1, 1, 1, 1)
                )
                .andExpect(status().isAccepted())
                .andDo(document(
                                "spexare/activities/tasks/update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("taskId").description("The id of the task"),
                                        parameterWithName("id").description("The id of the task activity")
                                )
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        when(service.deleteById(any(Long.class), any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{id}", 1, 1, 1)
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare/activities/tasks/delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the task activity")
                                )
                        )
                );
    }

    @Test
    public void should_get_task() throws Exception {
        var task = TaskDto.builder().id(1L).name("Scenmästare").build();
        var realTaskApi = new TaskApi(null, null, null, null);

        when(service.findTaskByTaskActivity(any(Long.class), any(Long.class), any(Long.class))).thenReturn(Optional.of(task));
        when(taskApi.getLinks(any(TaskDto.class))).thenReturn(realTaskApi.getLinks(task));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{id}/task", 1, 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/activities/tasks/get-task",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the task activity")
                                ),
                                taskResponseFields,
                                taskLinks,
                                responseHeaders
                        )
                );
    }

}
