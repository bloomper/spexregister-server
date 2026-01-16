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

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nu.fgv.register.server.util.impex.util.excel.ImpexUtil.determinePosition;
import static nu.fgv.register.server.util.impex.util.excel.ImpexUtil.determinePositionBeforeAuditableFields;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class ExcelReader {

    public <T> List<T> read(final Sheet sheet, final Class<T> clazz) {
        final List<T> results = new ArrayList<>();
        final List<Field> annotatedFields = Arrays.stream(FieldUtils.getAllFields(clazz))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .toList();

        final int maxPosition = determinePositionBeforeAuditableFields(annotatedFields);

        sheet.rowIterator().forEachRemaining(row -> {
            if (row.getRowNum() == 0) return; // Skip header
            results.add(mapRowToDto(row, clazz, annotatedFields, maxPosition));
        });

        return results;
    }

    public <T> T mapRowToDto(final Row row, final Class<T> clazz, final List<Field> fields, final int maxPosition) {
        try {
            final T dto = clazz.getDeclaredConstructor().newInstance();
            for (final Field field : fields) {
                final ExcelCell annotation = field.getAnnotation(ExcelCell.class);
                final int position = determinePosition(field, maxPosition);
                final Cell cell = row.getCell(position);

                if (cell != null) {
                    setCellValueToField(dto, field, cell);
                }
            }
            return dto;
        } catch (Exception e) {
            log.error("Failed to map Excel row {} to class {}", row.getRowNum(), clazz.getSimpleName(), e);
            throw new RuntimeException("Mapping error at row " + row.getRowNum(), e);
        }
    }

    private void setCellValueToField(final Object dto, final Field field, final Cell cell) throws IllegalAccessException {
        field.setAccessible(true);
        final Class<?> type = field.getType();

        if (type == String.class) {
            field.set(dto, getCellStringValue(cell));
        } else if (type == Long.class || type == long.class) {
            field.set(dto, (long) cell.getNumericCellValue());
        } else if (type == Boolean.class || type == boolean.class) {
            field.set(dto, cell.getBooleanCellValue());
        } else if (type == Integer.class || type == int.class) {
            field.set(dto, (int) cell.getNumericCellValue());
        }
    }

    private String getCellStringValue(final Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}