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

package nu.fgv.register.server.impex.importing.excel;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.importing.ImportEngine;
import nu.fgv.register.server.impex.importing.ImportEngineResponse;
import nu.fgv.register.server.impex.importing.ImportSpec;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.impex.model.excel.ExcelSheet;
import nu.fgv.register.server.util.Constants;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class ExcelImportEngine implements ImportEngine {

    private final MessageSource messageSource;
    private final ExcelReader excelReader;
    private final Validator beanValidator;

    @Override
    public ImportEngineResponse process(final byte[] file, final List<ImportSpec> specs, final Locale locale) {
        final ExcelValidator validator = new ExcelValidator(excelReader);

        try (final Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            final List<String> allMessages = new ArrayList<>();
            final Map<String, String> sheetNameMap = new HashMap<>();

            for (final ImportSpec spec : specs) {
                final String sheetName = getSheetName(spec.getClazz(), locale, spec.getName());
                sheetNameMap.put(spec.getClazz().getName(), sheetName);

                final ImportResultDto result = validator.validateSheet(messageSource, locale, workbook, sheetName, spec, beanValidator);

                if (!result.isSuccess()) {
                    allMessages.addAll(result.getMessages());
                }
            }

            final ImportResultDto validationResult = ImportResultDto.builder()
                    .success(allMessages.isEmpty())
                    .messages(allMessages)
                    .build();

            Map<Class<?>, List<?>> data = null;

            if (validationResult.isSuccess()) {
                data = new HashMap<>();
                for (final ImportSpec spec : specs) {
                    final Sheet sheet = workbook.getSheet(sheetNameMap.get(spec.getClazz().getName()));

                    if (sheet != null) {
                        data.put(spec.getClazz(), excelReader.read(sheet, spec.getClazz()));
                    }
                }
            }

            return new ImportEngineResponse(validationResult, data);
        } catch (final Exception e) {
            return new ImportEngineResponse(
                    ImportResultDto.builder()
                            .success(false)
                            .messages(List.of(e.getMessage()))
                            .build(),
                    null
            );
        }
    }

    @Override
    public boolean supports(final String contentType) {
        return Constants.MediaTypes.APPLICATION_XLSX_VALUE.equals(contentType) ||
                Constants.MediaTypes.APPLICATION_XLS_VALUE.equals(contentType);
    }

    private String getSheetName(final Class<?> clazz, final Locale locale, @Nullable final String override) {
        if (hasText(override)) {
            return messageSource.getMessage(override, null, override, locale);
        }
        if (clazz.isAnnotationPresent(ExcelSheet.class)) {
            final String key = clazz.getAnnotation(ExcelSheet.class).name();
            return messageSource.getMessage(key, null, key, locale);
        }
        return clazz.getSimpleName();
    }
}