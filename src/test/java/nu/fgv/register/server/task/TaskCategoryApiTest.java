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
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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

@WebMvcTest(value = TaskCategoryApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class TaskCategoryApiTest extends AbstractApiTest {

    @MockBean
    private TaskCategoryService service;

    @MockBean
    private TaskCategoryImportService importService;

    @MockBean
    private TaskCategoryExportService exportService;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task category"),
            fieldWithPath("name").description("The name of the task category"),
            fieldWithPath("hasActor").description("The flag telling whether the task category can have associated actor information"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and();

    @Test
    public void should_get_paged_task_categories() throws Exception {
        var category1 = TaskCategoryDto.builder().id(1L).name("category1").build();
        var category2 = TaskCategoryDto.builder().id(2L).name("category2").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category1, category2), PageRequest.of(1, 2, Sort.by("name")), 10));

        this.mockMvc
                .perform(
                        get("/api/v1/task-categories?page=1&size=2&sort=name,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.taskCategories", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "task-categories/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.taskCategories[]").description("The elements"),
                                        fieldWithPath("_embedded.taskCategories[].id").description("The id of the task category"),
                                        fieldWithPath("_embedded.taskCategories[].name").description("The name of the task category"),
                                        fieldWithPath("_embedded.taskCategories[].hasActor").description("The flag telling whether the task category can have associated actor information"),
                                        fieldWithPath("_embedded.taskCategories[].createdBy").description("Who created the task category"),
                                        fieldWithPath("_embedded.taskCategories[].createdAt").description("When was the task category created"),
                                        fieldWithPath("_embedded.taskCategories[].lastModifiedBy").description("Who last modified the task category"),
                                        fieldWithPath("_embedded.taskCategories[].lastModifiedAt").description("When was the task category last modified"),
                                        subsectionWithPath("_embedded.taskCategories[]._links").description("The task category links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_task_categories_export() throws Exception {
        var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        this.mockMvc
                .perform(
                        get("/api/v1/task-categories?ids=1,2,3")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "task-categories/get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the task categories to export").optional()
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
    public void should_create_task_category() throws Exception {
        var fields = new ConstrainedFields(TaskCategoryCreateDto.class);
        var dto = TaskCategoryCreateDto.builder().hasActor(false).name("Scenmästare").build();

        when(service.create(any(TaskCategoryCreateDto.class))).thenReturn(TaskCategoryDto.builder().id(1L).hasActor(dto.isHasActor()).name(dto.getName()).build());

        this.mockMvc
                .perform(
                        post("/api/v1/task-categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "task-categories/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the task category"),
                                        fields.withPath("hasActor").description("The flag telling whether the task category can have associated actor information")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_task_category() throws Exception {
        var category = TaskCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        get("/api/v1/task-categories/{id}", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "task-categories/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task category")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update_task_category() throws Exception {
        var fields = new ConstrainedFields(TaskCategoryUpdateDto.class);
        var category = TaskCategoryDto.builder().id(1L).name("category").build();
        var dto = TaskCategoryUpdateDto.builder().id(1L).hasActor(true).name("Scenmästare").build();

        when(service.update(any(TaskCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        put("/api/v1/task-categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "task-categories/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task category")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the task category"),
                                        fields.withPath("name").description("The name of the task category"),
                                        fields.withPath("hasActor").description("The flag telling whether the task category can have associated actor information")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update_task_category() throws Exception {
        var fields = new ConstrainedFields(TaskCategoryUpdateDto.class);
        var category = TaskCategoryDto.builder().id(1L).name("category").hasActor(false).build();
        var dto = TaskCategoryUpdateDto.builder().id(1L).hasActor(false).build();

        when(service.partialUpdate(any(TaskCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        this.mockMvc
                .perform(
                        patch("/api/v1/task-categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "task-categories/partial-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task category")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the task category"),
                                        fields.withPath("name").description("The name of the task category").optional(),
                                        fields.withPath("hasActor").description("The flag telling whether the task category can have associated actor information").optional()
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete_task_category() throws Exception {
        var category = TaskCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));
        doNothing().when(service).deleteById(any(Long.class));

        this.mockMvc
                .perform(
                        delete("/api/v1/task-categories/{id}", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "task-categories/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task category")
                                )
                        )
                );
    }

}