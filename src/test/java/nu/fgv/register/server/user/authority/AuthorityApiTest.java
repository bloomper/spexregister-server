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

package nu.fgv.register.server.user.authority;

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
@WebMvcTest(value = AuthorityApi.class)
class AuthorityApiTest extends AbstractApiTest {

    @MockitoBean
    private AuthorityService service;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the authority"),
            fieldWithPath("label").description("The label of the authority"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("authorities").description("Link to authorities").optional()
    );

    @Test
    void should_get_all() throws Exception {
        final var authority1 = AuthorityDto.builder().id("ROLE_ADMIN").label("Administrator").build();
        final var authority2 = AuthorityDto.builder().id("ROLE_USER").label("User").build();

        when(service.findAll(any(Sort.class))).thenReturn(List.of(authority1, authority2));

        mockMvc
                .perform(
                        get("/api/users/authorities?sort=name,desc")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.authorities", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "user-authority-get-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.authorities[]").description("The elements"),
                                        fieldWithPath("_embedded.authorities[].id").description("The id of the authority"),
                                        fieldWithPath("_embedded.authorities[].label").description("The type of the authority"),
                                        fieldWithPath("_embedded.authorities[].createdBy").description("Who created the authority"),
                                        fieldWithPath("_embedded.authorities[].createdAt").description("When was the authority created"),
                                        fieldWithPath("_embedded.authorities[].lastModifiedBy").description("Who last modified the authority"),
                                        fieldWithPath("_embedded.authorities[].lastModifiedAt").description("When was the authority last modified"),
                                        subsectionWithPath("_embedded.authorities[]._links").description("The authority links"),
                                        linksSubsection
                                ),
                                sortQueryParameters,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(AuthorityApi.class, "retrieve", Sort.class))
                        )
                );
    }

    @Test
    void should_get() throws Exception {
        final var authority = AuthorityDto.builder().id("ROLE_ADMIN").label("Administrator").build();

        when(service.findById(any(String.class))).thenReturn(authority);

        mockMvc
                .perform(
                        get("/api/users/authorities/{id}", 1)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "user-authority-get-2",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the authority")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(AuthorityApi.class, "retrieve", String.class))
                        )
                );
    }

}
