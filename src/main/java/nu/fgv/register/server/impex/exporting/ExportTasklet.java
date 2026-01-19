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

package nu.fgv.register.server.impex.exporting;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.model.ExportType;
import nu.fgv.register.server.impex.model.ReportType;
import nu.fgv.register.server.impex.util.CountingIterable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@StepScope
@RequiredArgsConstructor
public class ExportTasklet implements Tasklet {

    private final ApplicationContext applicationContext;
    private final List<ExportEngine> engines;
    @Value("${spexregister.impex.storage-path}")
    private String storagePath;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        final Map<String, Object> params = chunkContext.getStepContext().getJobParameters();
        final StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();

        final String serviceBeanName = (String) params.get("serviceBeanName");
        final String filter = (String) params.get("filter");
        final String idsStr = (String) params.get("ids");
        final ExportType exportType = ExportType.valueOf((String) params.get("exportType"));
        final ReportType reportType = params.get("reportType") != null && hasText((String) params.get("reportType")) ? ReportType.valueOf((String) params.get("reportType")) : null;
        final Locale locale = Locale.forLanguageTag((String) params.get("locale"));

        final ExportService service = applicationContext.getBean(serviceBeanName, ExportService.class);
        final List<Long> ids = (idsStr == null || idsStr.isEmpty()) ?
                List.of() : Arrays.stream(idsStr.split(",")).map(Long::valueOf).toList();

        final List<ExportHolder<?>> holders = service.doExport(ids, filter);
        final List<ExportHolder<?>> countingHolders = holders.stream()
                .<ExportHolder<?>>map(h -> wrapWithCounting(h, stepExecution))
                .toList();

        final ExportEngine engine = engines.stream()
                .filter(e -> e.supports(exportType))
                .findFirst()
                .orElseThrow();
        final byte[] binary = engine.export(countingHolders, exportType, reportType, locale);
        final String fileName = "export-" + chunkContext.getStepContext().getJobInstanceId() + engine.getExtension(exportType);
        final Path path = Paths.get(storagePath, "exports", fileName);

        Files.createDirectories(path.getParent());
        Files.write(path, binary);

        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().putString("outputFilePath", path.toString());

        return RepeatStatus.FINISHED;
    }

    private <T> ExportHolder<T> wrapWithCounting(final ExportHolder<T> holder, final StepExecution stepExecution) {
        return new ExportHolder<>(
                holder.name(),
                new CountingIterable<>(holder.data(), stepExecution),
                holder.clazz(),
                holder.readOnly()
        );
    }
}
