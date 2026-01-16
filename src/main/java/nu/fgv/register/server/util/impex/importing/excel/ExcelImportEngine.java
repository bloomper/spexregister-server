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

package nu.fgv.register.server.util.impex.importing.excel;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.error.ImportException;
import nu.fgv.register.server.util.impex.importing.ImportEngine;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import nu.fgv.register.server.util.impex.model.excel.ExcelSheet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class ExcelImportEngine implements ImportEngine {

    private final MessageSource messageSource;
    private final ExcelReader excelReader;
    private final ExcelValidator validator = new ExcelValidator();

    @Override
    public <T> ImportResultDto validate(final byte[] file, final Class<T> clazz, final Locale locale, final Function<Long, Boolean> existenceChecker) {
        try (final Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            return validator.validateSheet(messageSource, locale, workbook, clazz, existenceChecker);
        } catch (final Exception e) {
            return ImportResultDto.builder().success(false).messages(List.of(e.getMessage())).build();
        }
    }

    @Override
    public <T> List<T> parse(final byte[] file, final Class<T> clazz, final Locale locale) {
        try (final Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            final String sheetName = getSheetName(clazz, locale);
            final Sheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                return Collections.emptyList();
            }

            return excelReader.read(sheet, clazz);
        } catch (final Exception e) {
            throw new ImportException("Error parsing Excel file: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(final String contentType) {
        return Constants.MediaTypes.APPLICATION_XLSX_VALUE.equals(contentType) ||
                Constants.MediaTypes.APPLICATION_XLS_VALUE.equals(contentType);
    }

    private String getSheetName(final Class<?> clazz, final Locale locale) {
        if (clazz.isAnnotationPresent(ExcelSheet.class)) {
            final String key = clazz.getAnnotation(ExcelSheet.class).name();
            return messageSource.getMessage(key, null, key, locale);
        }
        return clazz.getSimpleName();
    }
}