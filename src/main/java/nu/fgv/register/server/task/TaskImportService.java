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
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.SpexCreateDto;
import nu.fgv.register.server.task.category.TaskCategoryService;
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ExcelValidator;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskImportService extends AbstractImportService {

    private final TaskService service;
    private final TaskCategoryService categoryService;
    private final MessageSource messageSource;
    private final ExcelValidator validator = new ExcelValidator();

    @Override
    protected ImportResultDto doImport(final Workbook workbook, final Locale locale) {
        return null;
    }

    @Override
    protected ImportResultDto doValidate(final Workbook workbook, final Locale locale) {
        final ImportResultDto validationResult = validator.validateSheet(messageSource, locale, workbook, TaskDto.class, SpexCreateDto.class, TaskUpdateDto.class, id -> service.findById(id).isPresent());
        final ImportResultDto categoryValidationResult = validator.validateSheet(messageSource, locale, workbook, SpexCategoryDto.class, id -> categoryService.findById(id).isPresent());
        final List<String> messages = Stream.concat(
                        validationResult.getMessages().stream(),
                        categoryValidationResult.getMessages().stream())
                .toList();

        return ImportResultDto.builder().success(messages.isEmpty()).messages(messages).build();
    }

}
