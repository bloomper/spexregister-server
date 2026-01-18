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

package nu.fgv.register.server.statistics;

import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = StatisticsApi.class)
class StatisticsApiTest extends AbstractApiTest {

    private final ResponseFieldsSnippet responseFields = responseFields(
            fieldWithPath("spexareCount").description("The total number of spexare"),
            fieldWithPath("spexareCountHistory").description("History of the number of spexare per month"),
            fieldWithPath("spexareCountHistory[].label").description("The month and year (YYYY-MM)"),
            fieldWithPath("spexareCountHistory[].count").description("The count for that month"),
            fieldWithPath("userCount").description("The total number of users"),
            fieldWithPath("userCountHistory").description("History of the number of users per month"),
            fieldWithPath("userCountHistory[].label").description("The month and year (YYYY-MM)"),
            fieldWithPath("userCountHistory[].count").description("The count for that month"),
            fieldWithPath("spexCount").description("The total number of spex"),
            fieldWithPath("spexCountHistory").description("History of the number of spex per month"),
            fieldWithPath("spexCountHistory[].label").description("The month and year (YYYY-MM)"),
            fieldWithPath("spexCountHistory[].count").description("The count for that month"),
            fieldWithPath("spexRevivalCount").description("The total number of spex revivals"),
            fieldWithPath("spexRevivalCountHistory").description("History of the number of spex revivals per month"),
            fieldWithPath("spexRevivalCountHistory[].label").description("The month and year (YYYY-MM)"),
            fieldWithPath("spexRevivalCountHistory[].count").description("The count for that month"),
            fieldWithPath("taskCount").description("The total number of tasks"),
            fieldWithPath("taskCountHistory").description("History of the number of tasks per month"),
            fieldWithPath("taskCountHistory[].label").description("The month and year (YYYY-MM)"),
            fieldWithPath("taskCountHistory[].count").description("The count for that month")
    );
    @MockitoBean
    private StatisticsService service;

    @Test
    void should_get() throws Exception {
        final StatisticsDto statistics = StatisticsDto.builder()
                .spexareCount(10L)
                .spexareCountHistory(List.of(new HistoryDto("2023-01", 2L)))
                .userCount(5L)
                .userCountHistory(List.of(new HistoryDto("2023-01", 1L)))
                .spexCount(3L)
                .spexCountHistory(List.of(new HistoryDto("2023-01", 1L)))
                .spexRevivalCount(1L)
                .spexRevivalCountHistory(List.of(new HistoryDto("2023-01", 0L)))
                .taskCount(20L)
                .taskCountHistory(List.of(new HistoryDto("2023-01", 5L)))
                .build();

        when(service.getStatistics()).thenReturn(statistics);

        mockMvc
                .perform(
                        get("/api/statistics")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spexareCount", is(10)))
                .andDo(print())
                .andDo(
                        document(
                                "statistics-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(StatisticsApi.class, "getStatistics"))
                        )
                );
    }

}
