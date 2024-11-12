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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryService;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExcelWriter;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TaskExportService extends AbstractExportService {

    private final TaskService service;
    private final TaskCategoryService categoryService;
    private final MessageSource messageSource;
    private final ExcelWriter writer = new ExcelWriter();

    @Override
    protected byte[] doExport(final Workbook workbook, final List<Long> ids, final Locale locale) {
        final var dtos = retrieveDtos(ids);
        final var categoryDtos = retrieveCategoryDtos();

        writer.createSheet(messageSource, locale, workbook, dtos);
        writer.createSheet(messageSource, locale, workbook, categoryDtos)
                .ifPresent(sheet -> {
                    if (sheet instanceof final XSSFSheet sheet0) {
                        final byte[] red = DefaultIndexedColorMap.getDefaultRGB(IndexedColors.RED.getIndex());
                        sheet0.setTabColor(new XSSFColor(red));
                    }
                    sheet.protectSheet("");
                });

        return convertWorkbookToByteArray(workbook);
    }

    private List<TaskDto> retrieveDtos(final List<Long> ids) {
        if (ids.isEmpty()) {
            return service.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else {
            return service.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

    private List<TaskCategoryDto> retrieveCategoryDtos() {
        return categoryService.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
