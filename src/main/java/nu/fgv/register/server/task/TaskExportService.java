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
import nu.fgv.register.server.task.category.TaskCategoryImpexDto;
import nu.fgv.register.server.task.category.TaskCategoryService;
import nu.fgv.register.server.impex.exporting.AbstractExportService;
import nu.fgv.register.server.impex.exporting.ExportEngine;
import nu.fgv.register.server.impex.exporting.ExportHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;
import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class TaskExportService extends AbstractExportService {

    private final TaskService service;
    private final TaskCategoryService categoryService;

    public TaskExportService(final TaskService service, final TaskCategoryService categoryService, final List<ExportEngine> engines) {
        super(engines);
        this.service = service;
        this.categoryService = categoryService;
    }

    @Override
    protected List<ExportHolder<?>> getReports(final List<Long> ids, final String filter) {
        return List.of(
                ExportHolder.of(
                        toImpexDto(service.streamByIds(ids, filter, Sort.by(Sort.Direction.ASC, "name")), TASK_MAPPER::toImpexDto),
                        TaskImpexDto.class
                ),
                ExportHolder.of(
                        toImpexDto(categoryService.streamByIds(Collections.emptyList(), "", Sort.by(Sort.Direction.ASC, "name")), TASK_CATEGORY_MAPPER::toImpexDto),
                        TaskCategoryImpexDto.class,
                        true
                )
        );
    }

}
