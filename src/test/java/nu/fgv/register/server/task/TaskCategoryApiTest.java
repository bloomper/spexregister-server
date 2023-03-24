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

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("task-categories").description("Link to paged task categories").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var category1 = TaskCategoryDto.builder().id(1L).name("category1").build();
        var category2 = TaskCategoryDto.builder().id(2L).name("category2").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category1, category2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/v1/tasks/categories?page=1&size=2&sort=name,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.task-categories", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/categories/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.task-categories[]").description("The elements"),
                                        fieldWithPath("_embedded.task-categories[].id").description("The id of the task category"),
                                        fieldWithPath("_embedded.task-categories[].name").description("The name of the task category"),
                                        fieldWithPath("_embedded.task-categories[].hasActor").description("The flag telling whether the task category can have associated actor information"),
                                        fieldWithPath("_embedded.task-categories[].createdBy").description("Who created the task category"),
                                        fieldWithPath("_embedded.task-categories[].createdAt").description("When was the task category created"),
                                        fieldWithPath("_embedded.task-categories[].lastModifiedBy").description("Who last modified the task category"),
                                        fieldWithPath("_embedded.task-categories[].lastModifiedAt").description("When was the task category last modified"),
                                        subsectionWithPath("_embedded.task-categories[]._links").description("The task category links"),
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
                        get("/api/v1/tasks/categories?ids=1,2,3")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/categories/get-export",
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
    public void should_create() throws Exception {
        var fields = new ConstrainedFields(TaskCategoryCreateDto.class);
        var dto = TaskCategoryCreateDto.builder().hasActor(false).name("Scenmästare").build();

        when(service.create(any(TaskCategoryCreateDto.class))).thenReturn(TaskCategoryDto.builder().id(1L).hasActor(dto.isHasActor()).name(dto.getName()).build());

        mockMvc
                .perform(
                        post("/api/v1/tasks/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "tasks/categories/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("name").description("The name of the task category"),
                                        fields.withPath("hasActor").description("The flag telling whether the task category can have associated actor information")
                                ),
                                responseFields,
                                links,
                                createResponseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var category = TaskCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        get("/api/v1/tasks/categories/{id}", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/categories/get",
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
    public void should_update() throws Exception {
        var fields = new ConstrainedFields(TaskCategoryUpdateDto.class);
        var category = TaskCategoryDto.builder().id(1L).name("category").build();
        var dto = TaskCategoryUpdateDto.builder().id(1L).hasActor(true).name("Scenmästare").build();

        when(service.update(any(TaskCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        put("/api/v1/tasks/categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/categories/update",
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
    public void should_partial_update() throws Exception {
        var fields = new ConstrainedFields(TaskCategoryUpdateDto.class);
        var category = TaskCategoryDto.builder().id(1L).name("category").hasActor(false).build();
        var dto = TaskCategoryUpdateDto.builder().id(1L).hasActor(false).build();

        when(service.partialUpdate(any(TaskCategoryUpdateDto.class))).thenReturn(Optional.of(category));

        mockMvc
                .perform(
                        patch("/api/v1/tasks/categories/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "tasks/categories/partial-update",
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
    public void should_delete() throws Exception {
        var category = TaskCategoryDto.builder().id(1L).name("category").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(category));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/tasks/categories/{id}", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "tasks/categories/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task category")
                                )
                        )
                );
    }

}
