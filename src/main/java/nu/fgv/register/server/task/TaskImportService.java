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

package nu.fgv.register.server.task;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ImportEngine;
import nu.fgv.register.server.util.impex.importing.ImportSpec;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class TaskImportService extends AbstractImportService {

    private final TaskService service;

    public TaskImportService(final List<ImportEngine> engines,
                             final TaskService service,
                             final MessageSource messageSource) {
        super(engines, messageSource);
        this.service = service;
    }

    @Override
    protected List<ImportSpec> getImportSpecs() {
        return List.of(
                ImportSpec.builder()
                        .clazz(TaskImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> service.exists((Long) v)
                        ))
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data, final Locale locale) {
        final ImportSummary summary = new ImportSummary(messageSource, locale);

        handleImport(
                (List<TaskImpexDto>) data.get(TaskImpexDto.class),
                summary,
                "task.impex.entityName",
                dto -> {
                    final TaskDto task = service.create(TASK_MAPPER.toCreateDto(dto));

                    service.addCategory(task.getId(), dto.getCategoryId());
                },
                dto -> {
                    final TaskDto task = service.partialUpdate(TASK_MAPPER.toUpdateDto(dto));

                    if (!service.findCategoryByTask(task.getId()).getId().equals(dto.getCategoryId())) {
                        service.addCategory(task.getId(), dto.getCategoryId());
                    }
                },
                dto -> service.deleteById(dto.getId())
        );

        return summary.toResult();
    }
}
