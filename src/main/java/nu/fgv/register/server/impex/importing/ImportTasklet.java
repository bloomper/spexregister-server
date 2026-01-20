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

package nu.fgv.register.server.impex.importing;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.ImportResultDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@StepScope
@RequiredArgsConstructor
public class ImportTasklet implements Tasklet {

    private final ApplicationContext applicationContext;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        final Map<String, Object> params = chunkContext.getStepContext().getJobParameters();

        final String serviceBeanName = (String) params.get("serviceBeanName");
        final String filePath = (String) params.get("filePath");
        final ImpexType type = ImpexType.valueOf((String) params.get("impexType"));
        final Locale locale = Locale.forLanguageTag((String) params.get("locale"));

        final AbstractImportService service = applicationContext.getBean(serviceBeanName, AbstractImportService.class);
        final Path path = Paths.get(filePath);
        final byte[] binary = Files.readAllBytes(path);

        final ImportResultDto result = service.doImport(binary, type, locale);

        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("importResult", result);

        return RepeatStatus.FINISHED;
    }

}
