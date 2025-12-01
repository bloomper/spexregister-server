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

package nu.fgv.register.server.user.state;

import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = StateApi.class)
class StateApiTest extends AbstractApiTest {

    @MockitoBean
    private StateService service;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the state"),
            fieldWithPath("label").description("The label of the state"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("states").description("Link to states").optional()
    );

    @Test
    void should_get_all() throws Exception {
        final var state1 = StateDto.builder().id("PENDING").label("Pending").build();
        final var state2 = StateDto.builder().id("ACTIVE").label("Active").build();

        when(service.findAll(any(Sort.class))).thenReturn(List.of(state1, state2));

        mockMvc
                .perform(
                        get("/api/v1/users/states?sort=name,desc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.states", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "user-state-get-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.states[]").description("The elements"),
                                        fieldWithPath("_embedded.states[].id").description("The id of the state"),
                                        fieldWithPath("_embedded.states[].label").description("The type of the state"),
                                        fieldWithPath("_embedded.states[].createdBy").description("Who created the state"),
                                        fieldWithPath("_embedded.states[].createdAt").description("When was the state created"),
                                        fieldWithPath("_embedded.states[].lastModifiedBy").description("Who last modified the state"),
                                        fieldWithPath("_embedded.states[].lastModifiedAt").description("When was the state last modified"),
                                        subsectionWithPath("_embedded.states[]._links").description("The state links"),
                                        linksSubsection
                                ),
                                sortQueryParameters,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(StateApi.class, "retrieve", Sort.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var state = StateDto.builder().id("PENDING").label("Pending").build();

        when(service.findById(any(String.class))).thenReturn(state);

        mockMvc
                .perform(
                        get("/api/v1/users/states/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "user-state-get-2",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the state")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(StateApi.class, "retrieve", String.class))
                        )
                );
    }

}
