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

package nu.fgv.register.server.impex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.impex.exporting.ExportService;
import nu.fgv.register.server.impex.importing.AbstractImportService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.impex.model.JobDto;
import nu.fgv.register.server.impex.model.JobStatusDto;
import nu.fgv.register.server.impex.model.ReportType;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final Job exportJob;
    private final Job importJob;
    private final ApplicationContext applicationContext;
    private final JdbcTemplate jdbcTemplate;
    @Value("${spexregister.jobs.job-cleanup.purge-threshold-in-days}")
    private int purgeThresholdInDays;
    @Value("${spexregister.impex.storage-path}")
    private String storagePath;

    public Long createExportJob(final Class<? extends ExportService> serviceClass, final List<Long> ids, final String filter, final ImpexType type, final Locale locale) {
        return createExportJob(serviceClass, ids, filter, type, null, locale);
    }

    public Long createExportJob(final Class<? extends ExportService> serviceClass, final List<Long> ids, final String filter, final ImpexType type, @Nullable final ReportType reportType, final Locale locale) {
        final String serviceBeanName = getServiceBeanName(serviceClass);
        final String requestor = getRequestor();
        final String authorities = getAuthorities();

        if (requestor != null) {
            final String idsParam = ids.stream().map(Object::toString).collect(Collectors.joining(","));
            final JobParameters jobParameters = new JobParametersBuilder()
                    .addString("serviceBeanName", serviceBeanName)
                    .addString("filter", filter)
                    .addString("ids", idsParam)
                    .addString("impexType", type.name())
                    .addString("reportType", reportType != null ? reportType.name() : "")
                    .addString("locale", locale.toLanguageTag())
                    .addString("requestor", requestor)
                    .addString("authorities", authorities)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            try {
                return jobOperator.start(exportJob, jobParameters).getJobInstance().getInstanceId();
            } catch (final Exception e) {
                log.error("Failed to create export job", e);
                throw new InternalErrorException("Failed to create export job");
            }
        } else {
            throw new InternalErrorException("No authenticated user");
        }
    }

    public Long createImportJob(final Class<? extends AbstractImportService> serviceClass, final byte[] file, final ImpexType type, final Locale locale) {
        final String serviceBeanName = getServiceBeanName(serviceClass);
        final String requestor = getRequestor();
        final String authorities = getAuthorities();

        if (requestor != null) {
            try {
                final String fileName = "import-" + System.currentTimeMillis();
                final Path path = Paths.get(storagePath, "imports", fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, file);

                final JobParameters jobParameters = new JobParametersBuilder()
                        .addString("serviceBeanName", serviceBeanName)
                        .addString("filePath", path.toString())
                        .addString("impexType", type.name())
                        .addString("locale", locale.toLanguageTag())
                        .addString("requestor", requestor)
                        .addString("authorities", authorities)
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();

                return jobOperator.start(importJob, jobParameters).getJobInstance().getInstanceId();
            } catch (final Exception e) {
                log.error("Failed to create import job", e);
                throw new InternalErrorException("Failed to create import job");
            }
        } else {
            throw new InternalErrorException("No authenticated user");
        }
    }

    public Mono<Resource> getJobOutputFile(final Long jobId) {
        return getJobExecution(jobId)
                .flatMap(execution -> {
                    if (!execution.getStatus().isLessThanOrEqualTo(BatchStatus.COMPLETED)) {
                        return Mono.error(new InternalErrorException("Job is not finished or failed"));
                    }

                    final String path = execution.getExecutionContext().getString("outputFilePath", null);

                    if (path == null) {
                        return Mono.error(new ResourceNoValueException("Job", "outputFilePath", jobId));
                    }

                    return Mono.just(new FileSystemResource(path));
                });
    }

    public Mono<JobStatusDto> getJobStatus(final Long jobId) {
        return getJobExecution(jobId)
                .map(this::mapToJobStatus);
    }

    public Mono<JobDto> getJob(final Long jobId) {
        return getJobExecution(jobId)
                .map(this::mapToJob);
    }

    public Mono<List<JobDto>> getJobs() {
        return currentAuthentication()
                .switchIfEmpty(Mono.error(new AccessDeniedException("Access denied")))
                .map(Authentication::getName)
                .map(currentUser -> {
                    final String sql = "SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION_PARAMS WHERE PARAMETER_NAME = 'requestor' AND PARAMETER_VALUE = ?";
                    final List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, currentUser);

                    return ids.stream()
                            .map(jobRepository::getJobExecution)
                            .filter(java.util.Objects::nonNull)
                            .sorted((e1, e2) -> e2.getCreateTime().compareTo(e1.getCreateTime()))
                            .map(this::mapToJob)
                            .toList();
                });
    }

    private Mono<Authentication> currentAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .switchIfEmpty(Mono.fromSupplier(() -> SecurityContextHolder.getContext().getAuthentication()))
                .filter(auth -> auth != null && auth.isAuthenticated());
    }

    private JobDto mapToJob(final JobExecution execution) {
        final String outputFilePath = execution.getExecutionContext().getString("outputFilePath", null);
        final boolean hasDownload = execution.getStatus() == BatchStatus.COMPLETED
                && outputFilePath != null
                && Files.exists(Paths.get(outputFilePath));

        final JobDto.JobDtoBuilder builder = JobDto.builder()
                .id(execution.getJobInstance().getInstanceId())
                .name(execution.getJobInstance().getJobName())
                .status(execution.getStatus().name())
                .exitStatus(execution.getExitStatus().getExitCode())
                .createdAt(execution.getCreateTime().toInstant(ZoneOffset.UTC))
                .startedAt(execution.getStartTime() != null ? execution.getStartTime().toInstant(ZoneOffset.UTC) : null)
                .finishedAt(execution.getEndTime() != null ? execution.getEndTime().toInstant(ZoneOffset.UTC) : null)
                .hasDownload(hasDownload);

        final Object importResult = execution.getExecutionContext().get("importResult");

        if (importResult instanceof final ImportResultDto resultDto) {
            builder.importResult(resultDto);
        }

        return builder.build();
    }

    private JobStatusDto mapToJobStatus(final JobExecution execution) {
        return JobStatusDto.builder()
                .id(execution.getJobInstance().getInstanceId())
                .name(execution.getJobInstance().getJobName())
                .status(execution.getStatus().name())
                .exitStatus(execution.getExitStatus().getExitCode())
                .build();
    }

    private Mono<JobExecution> getJobExecution(final Long jobId) {
        return currentAuthentication()
                .switchIfEmpty(Mono.error(new AccessDeniedException("Access denied")))
                .flatMap(auth ->
                        Mono.fromCallable(() -> jobRepository.getJobExecution(jobId))
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Job", jobId)))
                                .flatMap(execution -> {
                                    final String requestor = execution.getJobParameters().getString("requestor");

                                    if (requestor == null || !requestor.equals(auth.getName())) {
                                        return Mono.error(new AccessDeniedException("Access denied"));
                                    }
                                    return Mono.just(execution);
                                })
                );
    }

    @Scheduled(cron = "${spexregister.jobs.job-cleanup.cron-expression}")
    public void scheduledCleanup() {
        log.info("Starting job metadata and files cleanup job");
        purgeOldJobMetadata(purgeThresholdInDays);
        purgeOldFiles(Paths.get(storagePath, "exports"), purgeThresholdInDays);
        purgeOldFiles(Paths.get(storagePath, "imports"), purgeThresholdInDays);
    }

    private void purgeOldFiles(final Path directory, final int purgeThresholdInDays) {
        if (!Files.exists(directory)) {
            return;
        }

        try (final var stream = Files.list(directory)) {
            stream.filter(path -> {
                try {
                    return Files.getLastModifiedTime(path).toInstant()
                            .isBefore(Instant.now().minus(purgeThresholdInDays, ChronoUnit.DAYS));
                } catch (final IOException e) {
                    return false;
                }
            }).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (final IOException _) {
                }
            });
        } catch (final IOException e) {
            log.error("Failed to cleanup impex files", e);
        }
    }

    private String getServiceBeanName(final Class<?> serviceClass) {
        final String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, serviceClass);
        if (beanNames.length == 0) {
            throw new InternalErrorException("No bean found for service class: " + serviceClass.getName());
        }
        return beanNames[0];
    }

    private @Nullable String getRequestor() {
        return SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : null;
    }

    private String getAuthorities() {
        return SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")) : "";
    }

    private void purgeOldJobMetadata(final int purgeThresholdInDays) {
        final Instant threshold = Instant.now().minus(purgeThresholdInDays, ChronoUnit.DAYS);
        final java.sql.Timestamp timestamp = java.sql.Timestamp.from(threshold);

        final String[] queries = {
                "DELETE FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (SELECT STEP_EXECUTION_ID FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME < ?))",
                "DELETE FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME < ?)",
                "DELETE FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME < ?)",
                "DELETE FROM BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME < ?)",
                "DELETE FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME < ?",
                "DELETE FROM BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID NOT IN (SELECT JOB_INSTANCE_ID FROM BATCH_JOB_EXECUTION)"
        };

        for (final String query : queries) {
            try {
                if (query.contains("?")) {
                    jdbcTemplate.update(query, timestamp);
                } else {
                    jdbcTemplate.update(query);
                }
            } catch (final Exception e) {
                log.error("Failed to execute cleanup query: {}", query, e);
            }
        }
    }

}
