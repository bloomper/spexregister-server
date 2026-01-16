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

package nu.fgv.register.server.util.impex.importing.excel;

import jakarta.validation.ConstraintViolation;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import nu.fgv.register.server.util.impex.model.excel.ExcelSheet;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static nu.fgv.register.server.util.impex.util.excel.ImpexUtil.determinePosition;
import static nu.fgv.register.server.util.impex.util.excel.ImpexUtil.determinePositionBeforeAuditableFields;
import static nu.fgv.register.server.util.impex.util.excel.ImpexUtil.isMarkedForCreation;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class ExcelValidator {

    private final ExcelReader excelReader = new ExcelReader();

    public ImportResultDto validateSheet(
            final MessageSource messageSource,
            final Locale locale,
            final Workbook workbook,
            final Class<?> clazz,
            final Function<Long, Boolean> existenceChecker) {
        return validateSheet(messageSource, locale, workbook, clazz, existenceChecker, null);
    }

    public ImportResultDto validateSheet(
            final MessageSource messageSource,
            final Locale locale,
            final Workbook workbook,
            final Class<?> clazz,
            final Function<Long, Boolean> existenceChecker,
            @Nullable final String overrideSheetName) {

        final WorkbookContainer container = new WorkbookContainer();
        container.setMessageSource(messageSource);
        container.setLocale(locale);
        container.setWorkbook(workbook);
        container.setExistenceChecker(existenceChecker);

        final SheetContainer sheetContainer = new SheetContainer();
        sheetContainer.setOverrideSheetName(overrideSheetName);
        sheetContainer.setClazz(clazz);

        // Execute chain
        validateExistence(container, sheetContainer);
        if (container.getMessages().isEmpty()) {
            validateColumns(container, sheetContainer);
            validateEntries(container, sheetContainer);
        }

        return ImportResultDto.builder()
                .success(container.getMessages().isEmpty())
                .messages(container.getMessages())
                .build();
    }

    private void validateExistence(final WorkbookContainer workbookContainer, final SheetContainer sheetContainer) {
        final Class<?> clazz = sheetContainer.getClazz();
        final String sheetName;

        if (hasText(sheetContainer.getOverrideSheetName())) {
            sheetName = sheetContainer.getOverrideSheetName();
        } else if (clazz.isAnnotationPresent(ExcelSheet.class)) {
            final String name = clazz.getAnnotation(ExcelSheet.class).name();
            sheetName = workbookContainer.getMessageSource().getMessage(name, null, name, workbookContainer.getLocale());
        } else {
            sheetName = parseCamelCase(clazz.getSimpleName());
        }

        final int sheetIndex = workbookContainer.getWorkbook().getSheetIndex(sheetName);
        sheetContainer.setSheetIndex(sheetIndex);

        if (sheetIndex == -1) {
            workbookContainer.getMessages().add(workbookContainer.getMessageSource().getMessage("import.validation.missingSheet", new Object[]{sheetName}, workbookContainer.getLocale()));
        }
    }

    private void validateColumns(final WorkbookContainer workbookContainer, final SheetContainer sheetContainer) {
        final Sheet sheet = workbookContainer.getWorkbook().getSheetAt(sheetContainer.getSheetIndex());
        final List<Field> annotatedFields = Arrays.stream(FieldUtils.getAllFields(sheetContainer.getClazz()))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .toList();

        final int maxPosition = determinePositionBeforeAuditableFields(annotatedFields);

        annotatedFields.forEach(field -> {
            final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
            final int position = determinePosition(field, maxPosition);
            String expectedHeader = excelCell.header();

            if (!hasText(expectedHeader)) {
                expectedHeader = parseCamelCase(field.getName());
            } else {
                expectedHeader = workbookContainer.getMessageSource().getMessage(expectedHeader, null, expectedHeader, workbookContainer.getLocale());
            }

            final Cell actualCell = sheet.getRow(0).getCell(position);
            if (actualCell == null || !actualCell.getStringCellValue().equals(expectedHeader)) {
                workbookContainer.getMessages().add(workbookContainer.getMessageSource().getMessage("import.validation.columnMismatch", new Object[]{position, expectedHeader}, workbookContainer.getLocale()));
            }

            // Capture PK position (assuming position 0 or field "id")
            if (field.getName().equals("id") || position == 0) {
                sheetContainer.setPrimaryKeyPosition(position);
            }
        });
    }

    private void validateEntries(final WorkbookContainer workbookContainer, final SheetContainer sheetContainer) {
        final Sheet sheet = workbookContainer.getWorkbook().getSheetAt(sheetContainer.getSheetIndex());
        final List<Field> annotatedFields = Arrays.stream(FieldUtils.getAllFields(sheetContainer.getClazz()))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .toList();
        final int maxPosition = determinePositionBeforeAuditableFields(annotatedFields);

        sheet.rowIterator().forEachRemaining(row -> {
            if (row.getRowNum() == 0) return;

            final Cell pkCell = row.getCell(sheetContainer.getPrimaryKeyPosition());
            if (pkCell != null && !isMarkedForCreation(pkCell)) {
                final Long id = getNumericCellValue(pkCell);
                if (id != null && id > 0 && !workbookContainer.getExistenceChecker().apply(id)) {
                    workbookContainer.getMessages().add(workbookContainer.getMessageSource().getMessage("import.validation.entryDoesNotExist", new Object[]{row.getRowNum()}, workbookContainer.getLocale()));
                }
            }

            // Map row to DTO using the new reader
            final Object dto = excelReader.mapRowToDto(row, sheetContainer.getClazz(), annotatedFields, maxPosition);

            // Perform constraint validation
            final Set<ConstraintViolation<Object>> violations = workbookContainer.getValidator().validate(dto);
            for (final ConstraintViolation<Object> violation : violations) {
                workbookContainer.getMessages().add("Row " + (row.getRowNum() + 1) + ": " + violation.getMessage());
            }
        });
    }

    private void validateDtoConstraints(final WorkbookContainer workbookContainer, final SheetContainer sheetContainer, final Row row) {
        try {
            final Object dto = sheetContainer.getClazz().getDeclaredConstructor().newInstance();
            final Set<ConstraintViolation<Object>> violations = workbookContainer.getValidator().validate(dto);
            for (final ConstraintViolation<Object> violation : violations) {
                workbookContainer.getMessages().add("Row " + row.getRowNum() + ": " + violation.getMessage());
            }
        } catch (Exception e) {
            // Log mapping error
        }
    }

    private @Nullable Long getNumericCellValue(final Cell cell) {
        return switch (cell.getCellType()) {
            case STRING ->
                    hasText(cell.getStringCellValue()) ? Long.parseLong(cell.getStringCellValue().replaceAll("[^0-9]", "")) : null;
            case NUMERIC -> (long) cell.getNumericCellValue();
            default -> null;
        };
    }
}