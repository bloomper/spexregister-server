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

package nu.fgv.register.server.task.category;

import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
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
import java.util.UUID;

import static nu.fgv.register.server.util.TestUuids.uuid;
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
@WebMvcTest(value = TaskCategoryApi.class)
class TaskCategoryApiTest extends AbstractApiTest {

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the task category"),
            fieldWithPath("name").description("The name of the task category"),
            fieldWithPath("actorPresent").description("The flag telling whether the task category can have associated actor information"),
            fieldWithPath("board").description("Whether Spexipedia shows the category's tasks as the board of a spex"),
            fieldWithPath("retired").description("Whether the record is retired in Spexipedia: kept on activities, not offered for new ones"),
            linksSubsection
    );
    private final LinksSnippet links = baseLinks.and(
            linkWithRel("task-categories").description("Link to paged task categories").optional(),
            linkWithRel("revisions").description("Link to revisions").optional()
    );
    @MockitoBean
    private TaskCategoryService service;
    @MockitoBean
    private JobService jobService;

    @Test
    void should_get_paged() throws Exception {
        final var category1 = TaskCategoryDto.builder().id(uuid(1)).name("category1").build();
        final var category2 = TaskCategoryDto.builder().id(uuid(2)).name("category2").build();

        when(service.find(any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category1, category2), PageRequest.of(1, 2, Sort.by("name")), 10));

        mockMvc
                .perform(
                        get("/api/tasks/categories?page=1&size=2&sort=name,asc&filter=name:whatever")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.task-categories", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "task-category-get-all-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.task-categories[]").description("The elements"),
                                        fieldWithPath("_embedded.task-categories[].id").description("The id of the task category"),
                                        fieldWithPath("_embedded.task-categories[].name").description("The name of the task category"),
                                        fieldWithPath("_embedded.task-categories[].actorPresent").description("The flag telling whether the task category can have associated actor information"),
                                        fieldWithPath("_embedded.task-categories[].createdBy").description("Who created the task category"),
                                        fieldWithPath("_embedded.task-categories[].createdAt").description("When was the task category created"),
                                        fieldWithPath("_embedded.task-categories[].lastModifiedBy").description("Who last modified the task category"),
                                        fieldWithPath("_embedded.task-categories[].lastModifiedAt").description("When was the task category last modified"),
                                        subsectionWithPath("_embedded.task-categories[]._links").description("The task category links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TaskCategoryApi.class, "retrieve", Pageable.class, String.class))
                        )
                );
    }

    @Test
    void should_get_export() throws Exception {
        when(jobService.createExportJob(any(Class.class), anyList(), any(String.class), any(ImpexType.class), any(Locale.class))).thenReturn(1L);

        mockMvc
                .perform(
                        get("/api/tasks/categories?ids=" + uuid(1) + "," + uuid(2) + "," + uuid(3) + "&type=excel")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isAccepted())
                .andDo(print())
                .andDo(
                        document(
                                "task-category-get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("type").description("The impex type (excel, excel_xls and pdf supported)"),
                                        parameterWithName("ids").description("The ids of the task categories to export").optional(),
                                        parameterWithName("filter").description("The filter to use for the task categories to export").optional()
                                ),
                                exportResponseFields,
                                security(getRolesFromMethod(TaskCategoryApi.class, "retrieve", List.class, String.class, String.class, Locale.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var category = TaskCategoryDto.builder().id(uuid(1)).name("category").build();

        when(service.findById(any(UUID.class))).thenReturn(category);

        mockMvc
                .perform(
                        get("/api/tasks/categories/{id}", uuid(1))
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "task-category-get-2",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the task category")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(TaskCategoryApi.class, "retrieve", UUID.class))
                        )
                );
    }

}
