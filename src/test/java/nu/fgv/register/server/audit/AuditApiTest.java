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

package nu.fgv.register.server.audit;

import nu.fgv.register.server.util.AbstractApiTest;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
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
@WebMvcTest(value = AuditApi.class)
class AuditApiTest extends AbstractApiTest {

    @MockitoBean
    private AuditService service;

    private static RevisionDto revision(final long revision, final RevisionType revisionType) {
        return RevisionDto.builder()
                .revision(revision)
                .type(AuditedType.TAG)
                .entityId(1L)
                .entityLabel("tag2")
                .revisionType(revisionType)
                .modifiedAt(Instant.parse("2026-09-08T14:02:00Z"))
                .modifiedBy("anna@spexregister.com")
                .changes(List.of(FieldChangeDto.builder()
                        .field("name")
                        .oldValue("tag1")
                        .newValue("tag2")
                        .binary(false)
                        .build()))
                .build();
    }

    @Test
    void should_get_revisions() throws Exception {
        when(service.findRevisions(any(AuditedType.class), anyString()))
                .thenReturn(List.of(revision(2L, RevisionType.MOD), revision(1L, RevisionType.ADD)));

        mockMvc
                .perform(
                        get("/api/revisions/{type}/{id}", "TAG", "1")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.revisions", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "audit-get-revisions",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.revisions[]").description("The revisions"),
                                        fieldWithPath("_embedded.revisions[].revision").description("The revision number"),
                                        fieldWithPath("_embedded.revisions[].type").description("The audited type"),
                                        fieldWithPath("_embedded.revisions[].entityId").description("The id of the audited entity"),
                                        fieldWithPath("_embedded.revisions[].entityLabel").description("A human readable name for the audited entity").optional(),
                                        fieldWithPath("_embedded.revisions[].revisionType").description("The kind of change (ADD, MOD or DEL)"),
                                        fieldWithPath("_embedded.revisions[].modifiedAt").description("When the change was made"),
                                        fieldWithPath("_embedded.revisions[].modifiedBy").description("Who made the change"),
                                        subsectionWithPath("_embedded.revisions[].changes").description("The audited properties that changed"),
                                        subsectionWithPath("_embedded.revisions[]._links").description("The revision links"),
                                        subsectionWithPath("_links").description("The links")
                                ),
                                pathParameters(
                                        parameterWithName("type").description("The audited type"),
                                        parameterWithName("id").description("The id of the audited entity")
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(AuditApi.class, "retrieve", AuditedType.class, String.class))
                        )
                );
    }

    @Test
    void should_get_revision() throws Exception {
        when(service.findRevision(any(AuditedType.class), anyString(), anyLong()))
                .thenReturn(revision(2L, RevisionType.MOD));

        mockMvc
                .perform(
                        get("/api/revisions/{type}/{id}/{revision}", "TAG", "1", 2L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("revision", is(2)))
                .andDo(print())
                .andDo(
                        document(
                                "audit-get-revision",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        fieldWithPath("revision").description("The revision number"),
                                        fieldWithPath("type").description("The audited type"),
                                        fieldWithPath("entityId").description("The id of the audited entity"),
                                        fieldWithPath("entityLabel").description("A human readable name for the audited entity").optional(),
                                        fieldWithPath("revisionType").description("The kind of change (ADD, MOD or DEL)"),
                                        fieldWithPath("modifiedAt").description("When the change was made"),
                                        fieldWithPath("modifiedBy").description("Who made the change"),
                                        subsectionWithPath("changes").description("The audited properties that changed"),
                                        subsectionWithPath("_links").description("The links")
                                ),
                                pathParameters(
                                        parameterWithName("type").description("The audited type"),
                                        parameterWithName("id").description("The id of the audited entity"),
                                        parameterWithName("revision").description("The revision number")
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(AuditApi.class, "retrieve", AuditedType.class, String.class, Long.class))
                        )
                );
    }

    @Test
    void should_restore() throws Exception {
        when(service.restore(any(AuditedType.class), anyString(), anyLong(), anyBoolean()))
                .thenReturn(RestoreResultDto.builder()
                        .revision(2L)
                        .updated(1)
                        .created(0)
                        .deleted(0)
                        .warnings(List.of())
                        .build());

        mockMvc
                .perform(
                        post("/api/revisions/{type}/{id}/{revision}/restore?cascade=false", "TAG", "1", 2L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("updated", is(1)))
                .andDo(print())
                .andDo(
                        document(
                                "audit-restore",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        fieldWithPath("revision").description("The revision that was restored"),
                                        fieldWithPath("updated").description("How many entities were updated"),
                                        fieldWithPath("created").description("How many entities were re-created"),
                                        fieldWithPath("deleted").description("How many entities were removed"),
                                        subsectionWithPath("warnings").description("Non-fatal conditions encountered while restoring")
                                ),
                                pathParameters(
                                        parameterWithName("type").description("The audited type"),
                                        parameterWithName("id").description("The id of the audited entity"),
                                        parameterWithName("revision").description("The revision number to restore")
                                ),
                                queryParameters(
                                        parameterWithName("cascade").description("Whether to restore the children of the aggregate as well")
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(AuditApi.class, "restore", AuditedType.class, String.class, Long.class, Boolean.class))
                        )
                );
    }
}