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

package nu.fgv.register.server.impex;

import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.impex.model.JobDto;
import nu.fgv.register.server.impex.model.JobStatusDto;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = JobApi.class)
class JobApiTest extends AbstractApiTest {

    @MockitoBean
    private JobService service;
    @MockitoBean
    private JobProgressService progressService;

    @Test
    void should_get_status() throws Exception {
        final JobStatusDto statusDto = JobStatusDto.builder()
                .id(1L)
                .name("export")
                .status("COMPLETED")
                .exitStatus("COMPLETED")
                .build();

        when(service.getJobStatus(anyLong())).thenReturn(Mono.just(statusDto));

        final MvcResult mvcResult = mockMvc
                .perform(
                        get("/api/jobs/{id}/status", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(1)))
                .andExpect(jsonPath("status", is("COMPLETED")))
                .andDo(document(
                        "job-get-status",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("The id of the job")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the job"),
                                fieldWithPath("name").description("The name of the job"),
                                fieldWithPath("status").description("The status of the job"),
                                fieldWithPath("exitStatus").description("The exit status code")
                        ),
                        secureRequestHeaders,
                        security(getRolesFromMethod(JobApi.class, "status", Long.class))
                ));
    }

    @Test
    void should_get_job() throws Exception {
        final JobDto jobDto = JobDto.builder()
                .id(1L)
                .name("exportJob")
                .status("COMPLETED")
                .exitStatus("COMPLETED")
                .createdAt(Instant.now())
                .startedAt(Instant.now())
                .finishedAt(Instant.now())
                .importResult(ImportResultDto.builder().success(true).build())
                .build();

        when(service.getJob(anyLong())).thenReturn(Mono.just(jobDto));

        final MvcResult mvcResult = mockMvc
                .perform(
                        get("/api/jobs/{id}", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(1)))
                .andExpect(jsonPath("name", is("exportJob")))
                .andDo(document(
                        "job-get",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("The id of the job")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The id of the job"),
                                fieldWithPath("name").description("The name of the job"),
                                fieldWithPath("status").description("The status of the job"),
                                fieldWithPath("exitStatus").description("The exit status code"),
                                fieldWithPath("createdAt").description("When the job was created"),
                                fieldWithPath("startedAt").description("When the job started").optional(),
                                fieldWithPath("finishedAt").description("When the job finished").optional(),
                                fieldWithPath("hasDownload").description("Whether the job has a download or not"),
                                subsectionWithPath("importResult").description("The import result details (only for import jobs)").optional(),
                                fieldWithPath("importResult.success").description("Whether the import was successful").optional(),
                                fieldWithPath("importResult.messages").description("Summary messages").optional(),
                                fieldWithPath("importResult.errors").description("Error messages").optional(),
                                fieldWithPath("importResult.data").description("Additional result data").optional()
                        ),
                        secureRequestHeaders,
                        security(getRolesFromMethod(JobApi.class, "retrieve", Long.class))
                ));
    }

    @Test
    void should_retrieve_jobs() throws Exception {
        final JobDto jobDto = JobDto.builder()
                .id(1L)
                .name("exportJob")
                .status("COMPLETED")
                .exitStatus("COMPLETED")
                .createdAt(Instant.now())
                .build();

        when(service.getJobs()).thenReturn(Mono.just(List.of(jobDto)));

        final MvcResult mvcResult = mockMvc
                .perform(
                        get("/api/jobs")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andDo(document(
                        "job-get-all",
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].id").description("The id of the job"),
                                fieldWithPath("[].name").description("The name of the job"),
                                fieldWithPath("[].status").description("The status of the job"),
                                fieldWithPath("[].exitStatus").description("The exit status code"),
                                fieldWithPath("[].createdAt").description("When the job was created"),
                                fieldWithPath("[].startedAt").description("When the job started").optional(),
                                fieldWithPath("[].finishedAt").description("When the job finished").optional(),
                                fieldWithPath("[].hasDownload").description("Whether the job has a download or not"),
                                subsectionWithPath("[].importResult").description("The import result details (only for import jobs)").optional()
                        ),
                        secureRequestHeaders,
                        security(getRolesFromMethod(JobApi.class, "retrieve"))
                ));
    }

    @Test
    void should_get_progress() throws Exception {
        final JobStatusDto statusDto = JobStatusDto.builder().id(1L).name("export").status("COMPLETED").exitStatus("COMPLETED").build();
        final JobStatusDto pulse = JobStatusDto.builder().id(1L).name("export").status("STARTED").exitStatus("UNKNOWN").build();

        when(service.getJobStatus(anyLong())).thenReturn(Mono.just(statusDto));
        when(progressService.getStream(anyLong())).thenReturn(Flux.just(pulse));

        mockMvc
                .perform(
                        get("/api/jobs/{id}/progress", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                                .accept(MediaType.TEXT_EVENT_STREAM)
                )
                .andExpect(status().isOk())
                .andDo(document(
                        "job-get-progress",
                        pathParameters(
                                parameterWithName("id").description("The id of the job")
                        ),
                        secureRequestHeaders,
                        security(getRolesFromMethod(JobApi.class, "progress", Long.class))
                ));
    }

    @Test
    void should_get_results() throws Exception {
        final String filename = "export.xlsx";
        final var resource = new ByteArrayResource("content".getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        when(service.getJobOutputFile(anyLong())).thenReturn(Mono.just(resource));

        final MvcResult mvcResult = mockMvc
                .perform(
                        get("/api/jobs/{id}/results", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(filename)))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(document(
                        "job-get-results",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("The id of the job")
                        ),
                        secureRequestHeaders,
                        responseHeaders,
                        security(getRolesFromMethod(JobApi.class, "results", Long.class))
                ));
    }

}
