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
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.model.HasImpexAction;
import nu.fgv.register.server.util.impex.model.ImpexAction;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static nu.fgv.register.server.util.impex.util.excel.ImpexUtil.determinePosition;
import static nu.fgv.register.server.util.impex.util.excel.ImpexUtil.determinePositionBeforeAuditableFields;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelReader {

    private final MessageSource messageSource;

    public <T> List<T> read(final Sheet sheet, final Class<T> clazz) {
        final Map<Field, Integer> fieldColumnMap = buildFieldColumnMap(sheet, clazz);
        return read(sheet, clazz, fieldColumnMap);
    }

    public <T> List<T> read(final Sheet sheet, final Class<T> clazz, final Map<Field, Integer> fieldColumnMap) {
        final List<T> results = new ArrayList<>();
        final List<Field> annotatedFields = Arrays.stream(FieldUtils.getAllFields(clazz))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .peek(field -> field.setAccessible(true))
                .toList();

        final int maxPosition = determinePositionBeforeAuditableFields(annotatedFields);

        sheet.rowIterator().forEachRemaining(row -> {
            if (row.getRowNum() == 0 || isRowEmpty(row)) {
                return;
            }
            results.add(mapRowToDto(row, clazz, annotatedFields, maxPosition, fieldColumnMap));
        });

        return results;
    }

    public <T> T mapRowToDto(final Row row, final Class<T> clazz, final List<Field> fields, final int maxPosition, final Map<Field, Integer> fieldColumnMap) {
        try {
            final T dto = clazz.getDeclaredConstructor().newInstance();

            if (dto instanceof final HasImpexAction impexDto) {
                impexDto.setRowNumber(row.getRowNum() + 1);
            }

            fields.stream()
                    .filter(f -> f.getType() == ImpexAction.class)
                    .findFirst()
                    .ifPresent(field -> {
                        final Integer position = fieldColumnMap.getOrDefault(field, determinePosition(field, maxPosition, false));
                        if (position != null && position >= 0) {
                            final Cell cell = row.getCell(position);
                            if (cell != null) {
                                try {
                                    setCellValueToField(dto, field, cell);
                                } catch (IllegalAccessException e) {
                                    log.warn("Could not set action field", e);
                                }
                            }
                        }
                    });

            for (final Field field : fields) {
                final Integer position = fieldColumnMap.getOrDefault(field, determinePosition(field, maxPosition, false));
                if (position == null || position < 0) {
                    continue;
                }

                final Cell cell = row.getCell(position);

                if (cell != null) {
                    setCellValueToField(dto, field, cell);
                }
            }
            return dto;
        } catch (final Exception e) {
            log.error("Failed to map Excel row {} to class {}", row.getRowNum(), clazz.getSimpleName(), e);
            throw new RuntimeException("Mapping error at row " + row.getRowNum(), e);
        }
    }

    private Map<Field, Integer> buildFieldColumnMap(final Sheet sheet, final Class<?> clazz) {
        final Map<Field, Integer> fieldColumnMap = new HashMap<>();
        final Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return fieldColumnMap;
        }

        final Map<String, Integer> headerToIndexMap = new HashMap<>();
        headerRow.forEach(cell -> {
            if (cell.getCellType() == CellType.STRING) {
                headerToIndexMap.put(cell.getStringCellValue(), cell.getColumnIndex());
            }
        });

        final List<Field> fields = Arrays.stream(FieldUtils.getAllFields(clazz))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .peek(field -> field.setAccessible(true))
                .toList();

        final Locale locale = LocaleContextHolder.getLocale();
        fields.forEach(field -> {
            final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
            final String expectedHeader = hasText(excelCell.header()) ?
                    messageSource.getMessage(excelCell.header(), null, excelCell.header(), locale) :
                    parseCamelCase(field.getName());

            if (headerToIndexMap.containsKey(expectedHeader)) {
                fieldColumnMap.put(field, headerToIndexMap.get(expectedHeader));
            }
        });

        return fieldColumnMap;
    }

    private void setCellValueToField(final Object dto, final Field field, final Cell cell) throws IllegalAccessException {
        field.setAccessible(true);
        final Class<?> type = field.getType();

        if (type == String.class) {
            field.set(dto, getStringifiedCellValue(cell));
        } else if (type == Long.class || type == long.class) {
            field.set(dto, getLongifiedNumericCellValue(cell));
        } else if (type == Boolean.class || type == boolean.class) {
            field.set(dto, cell.getCellType() == CellType.BOOLEAN ? cell.getBooleanCellValue() : Boolean.parseBoolean(getStringifiedCellValue(cell)));
        } else if (type == Integer.class || type == int.class) {
            field.set(dto, getIntifiedNumericCellValue(cell));
        } else if (type == ImpexAction.class) {
            field.set(dto, parseImpexAction(cell));
        }
    }

    private ImpexAction parseImpexAction(final Cell cell) {
        final String value = getStringifiedCellValue(cell).trim().toUpperCase();

        if (value.equals("N") || value.equals("C") || value.equals("CREATE") || value.equals("NEW")) {
            return ImpexAction.CREATE;
        } else if (value.equals("D") || value.equals("R") || value.equals("DELETE") || value.equals("REMOVE")) {
            return ImpexAction.DELETE;
        }
        return ImpexAction.UPDATE;
    }

    private long getLongifiedNumericCellValue(final Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> (long) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Long.parseLong(cell.getStringCellValue().replaceAll("[^0-9]", ""));
                } catch (final NumberFormatException e) {
                    yield 0L;
                }
            }
            default -> 0L;
        };
    }

    private int getIntifiedNumericCellValue(final Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().replaceAll("[^0-9]", ""));
                } catch (final NumberFormatException e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    private String getStringifiedCellValue(final Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
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