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

package nu.fgv.register.server.impex.importing.excel;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.importing.ImportSpec;
import nu.fgv.register.server.impex.model.HasImpexAction;
import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.impex.model.excel.ExcelCell;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static nu.fgv.register.server.impex.util.excel.ImpexUtil.determinePosition;
import static nu.fgv.register.server.impex.util.excel.ImpexUtil.determinePositionBeforeAuditableFields;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
public class ExcelValidator {

    private final ExcelReader excelReader;

    public ImportResultDto validateSheet(
            final MessageSource messageSource,
            final Locale locale,
            final Workbook workbook,
            final String sheetName,
            final ImportSpec spec,
            final Validator validator) {

        final List<String> messages = new ArrayList<>();
        final Sheet sheet = workbook.getSheet(sheetName);

        if (sheet == null) {
            messages.add(messageSource.getMessage("impex.import.validation.missingSheet", new Object[]{sheetName}, locale));
            return ImportResultDto.builder()
                    .success(false)
                    .messages(messages)
                    .build();
        }

        final List<Field> annotatedFields = Arrays.stream(FieldUtils.getAllFields(spec.getClazz()))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .peek(field -> field.setAccessible(true))
                .toList();
        final int maxPosition = determinePositionBeforeAuditableFields(annotatedFields);

        final Map<Field, Integer> fieldColumnMap = new HashMap<>();
        validateColumns(messageSource, locale, sheet, annotatedFields, maxPosition, fieldColumnMap, messages);

        if (messages.isEmpty()) {
            validateRows(messageSource, locale, sheet, spec, annotatedFields, maxPosition, fieldColumnMap, validator, messages);
        }

        return ImportResultDto.builder()
                .success(messages.isEmpty())
                .messages(messages)
                .build();
    }

    private void validateColumns(final MessageSource messageSource,
                                 final Locale locale,
                                 final Sheet sheet,
                                 final List<Field> fields,
                                 final int maxPosition,
                                 final Map<Field, Integer> fieldColumnMap,
                                 final List<String> messages) {
        final Row headerRow = sheet.getRow(0);

        if (headerRow == null) {
            messages.add(messageSource.getMessage("impex.import.validation.missingSheet", new Object[]{sheet.getSheetName()}, locale));
            return;
        }

        final Map<String, Integer> headerToIndexMap = new HashMap<>();
        headerRow.forEach(cell -> {
            if (cell.getCellType() == CellType.STRING) {
                headerToIndexMap.put(cell.getStringCellValue(), cell.getColumnIndex());
            }
        });

        fields.forEach(field -> {
            final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
            final String expectedHeader = hasText(excelCell.header()) ?
                    messageSource.getMessage(excelCell.header(), null, excelCell.header(), locale) :
                    parseCamelCase(field.getName());

            if (headerToIndexMap.containsKey(expectedHeader)) {
                fieldColumnMap.put(field, headerToIndexMap.get(expectedHeader));
            } else if (excelCell.mandatory() || excelCell.primaryKey()) {
                final int logicalPos = determinePosition(field, maxPosition, false);
                messages.add(messageSource.getMessage("impex.import.validation.columnMismatch", new Object[]{logicalPos + 1, expectedHeader}, locale));
            }
        });
    }

    private void validateRows(final MessageSource messageSource,
                              final Locale locale,
                              final Sheet sheet,
                              final ImportSpec spec,
                              final List<Field> fields,
                              final int maxPosition,
                              final Map<Field, Integer> fieldColumnMap,
                              final Validator validator,
                              final List<String> messages) {
        final String rowTranslation = messageSource.getMessage("impex.import.validation.row", null, "Row", locale);

        sheet.rowIterator().forEachRemaining(row -> {
            if (row.getRowNum() == 0 || isRowEmpty(row)) {
                return;
            }

            final Object dto = excelReader.mapRowToDto(row, spec.getClazz(), fields, maxPosition, fieldColumnMap);
            final ImpexAction action = (dto instanceof final HasImpexAction impex) ? impex.getAction() : ImpexAction.UPDATE;
            final int rowNum = row.getRowNum() + 1;

            spec.getExistenceCheckers().forEach((fieldName, checker) ->
                    fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().ifPresent(field -> {
                        final ExcelCell annotation = field.getAnnotation(ExcelCell.class);
                        final Integer colIndex = fieldColumnMap.get(field);

                        if (colIndex != null) {
                            final Cell cell = row.getCell(colIndex);
                            final Object value = getCellValueByFieldType(cell, field.getType());

                            if (value != null) {
                                if (action == ImpexAction.CREATE && annotation.primaryKey()) {
                                    return;
                                }

                                if (!checker.apply(value)) {
                                    messages.add(String.format("%s %d: %s", rowTranslation, rowNum,
                                            messageSource.getMessage("impex.import.validation.entryDoesNotExist", null, locale)));
                                }
                            }
                        }
                    }));

            validator.validate(dto).forEach(v ->
                    messages.add(String.format("%s %d: %s", rowTranslation, rowNum, v.getMessage()))
            );
        });
    }

    private @Nullable Object getCellValueByFieldType(@Nullable final Cell cell, final Class<?> fieldType) {
        if (cell == null) {
            return null;
        }
        if (fieldType == String.class) {
            return getStringifiedCellValue(cell);
        }
        if (fieldType == Long.class || fieldType == long.class) {
            return getNumericCellValue(cell);
        }
        return null;
    }

    private String getStringifiedCellValue(final Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private @Nullable Long getNumericCellValue(final Cell cell) {
        return switch (cell.getCellType()) {
            case STRING ->
                    hasText(cell.getStringCellValue()) ? Long.parseLong(cell.getStringCellValue().replaceAll("[^0-9]", "")) : null;
            case NUMERIC -> (long) cell.getNumericCellValue();
            default -> null;
        };
    }

    private boolean isRowEmpty(@Nullable final Row row) {
        if (row == null) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            final Cell cell = row.getCell(c);

            if (cell != null && cell.getCellType() != CellType.BLANK && hasText(getStringifiedCellValue(cell))) {
                return false;
            }
        }
        return true;
    }
}