/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.spexare.bulk;

import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = SpexareBulkApi.class)
class SpexareBulkApiTest extends AbstractApiTest {

    private static final ResponseFieldsSnippet responseFields = responseFields(
            fieldWithPath("operation").description("The operation that was performed"),
            fieldWithPath("requested").description("The number of spexare in the target"),
            fieldWithPath("applied").description("The number of spexare that were, or would be, changed"),
            fieldWithPath("unchanged").description("The number of spexare that already had the requested state"),
            fieldWithPath("blocked").description("The number of spexare the caller may not modify"),
            subsectionWithPath("entries").description("The per-spexare outcome"),
            fieldWithPath("entries[].id").description("The id of the spexare"),
            fieldWithPath("entries[].label").description("The name of the spexare"),
            fieldWithPath("entries[].outcome").description("The outcome. One of `APPLIED`, `UNCHANGED`, `NOT_PERMITTED`"),
            fieldWithPath("entries[].detail").description("The names of what was, or would be, changed").optional(),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("preview").description("Link to preview a bulk operation"),
            linkWithRel("spexare").description("Link to all spexare")
    );

    private final ConstrainedFields fields = new ConstrainedFields(SpexareBulkInputDto.class);

    private final RequestFieldsSnippet requestFields = requestFields(
            subsection("target", "The spexare to act on"),
            fields.withPath("target.ids").description("The ids of the spexare to act on. Mutually exclusive with `target.filter`").optional(),
            fields.withPath("target.filter").description("The filter selecting the spexare to act on. Mutually exclusive with `target.ids`").optional(),
            fields.withPath("operation").description("The operation to perform. One of `TAG_ADD`, `TAG_REMOVE`, `SPEX_ADD`, `SPEX_REMOVE`, `TASK_ADD`, `TASK_REMOVE`, `CONSENT_SET`, `TOGGLE_SET`, `FIELDS_SET`"),
            fields.withPath("tags").description("The ids of the tags. Required for `TAG_ADD` and `TAG_REMOVE`").optional(),
            fields.withPath("spex").description("The ids of the spex. Required for `SPEX_ADD` and `SPEX_REMOVE`").optional(),
            fields.withPath("tasks").description("The ids of the tasks. Required for `TASK_ADD` and `TASK_REMOVE`").optional(),
            fields.withPath("spexId").description("The id of the spex whose activity the tasks belong to. Required for `TASK_ADD` and `TASK_REMOVE`").optional(),
            subsection("values", "The type/value pairs to set. Required for `CONSENT_SET` and `TOGGLE_SET`").optional(),
            subsection("fields", "The fields to set. Required for `FIELDS_SET`").optional()
    );

    @MockitoBean
    private SpexareBulkService service;

    private static FieldDescriptor subsection(final String path, final String description) {
        return subsectionWithPath(path).description(description).attributes(key("constraints").value(""));
    }

    private static SpexareBulkInputDto input() {
        return SpexareBulkInputDto.builder()
                .target(BulkTargetDto.builder().ids(List.of(1L, 2L)).build())
                .operation(SpexareBulkOperation.TAG_ADD)
                .tags(List.of(1L))
                .build();
    }

    private static BulkResultDto result() {
        return BulkResultDto.of(SpexareBulkOperation.TAG_ADD, List.of(
                BulkEntryDto.builder().id(1L).label("Anna Bergström").outcome(BulkOutcome.APPLIED).detail("Sångare").build(),
                BulkEntryDto.builder().id(2L).label("Bo Nilsson").outcome(BulkOutcome.UNCHANGED).build()
        ));
    }

    @Test
    void should_preview() throws Exception {
        when(service.preview(any(SpexareBulkInputDto.class))).thenReturn(result());

        mockMvc
                .perform(
                        post("/api/spexare/bulk/preview")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(input()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("applied", is(1)))
                .andExpect(jsonPath("unchanged", is(1)))
                .andDo(print())
                .andDo(document(
                                "spexare-bulk-preview",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields,
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareBulkApi.class, "preview", SpexareBulkInputDto.class))
                        )
                );
    }

    @Test
    void should_apply() throws Exception {
        when(service.apply(any(SpexareBulkInputDto.class))).thenReturn(result());

        mockMvc
                .perform(
                        post("/api/spexare/bulk")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .header("X-Audit-Reason", "Bulk action: Add tags (1 records)")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(input()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("applied", is(1)))
                .andDo(print())
                .andDo(document(
                                "spexare-bulk-apply",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields,
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(SpexareBulkApi.class, "apply", SpexareBulkInputDto.class))
                        )
                );
    }

}
