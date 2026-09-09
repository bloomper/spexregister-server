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

package nu.fgv.register.server.impex.exporting.excel;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.impex.model.excel.ExcelCell;
import nu.fgv.register.server.impex.model.excel.ExcelSheet;
import nu.fgv.register.server.impex.util.excel.ImpexUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
public class ExcelWriter {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    public <T> void createSheet(final MessageSource messageSource,
                                final Locale locale,
                                final Workbook workbook,
                                final Iterable<T> data,
                                @Nullable final String overrideSheetName,
                                final Class<T> clazz,
                                final boolean readOnly) {
        final List<T> dataList = StreamSupport.stream(data.spliterator(), false).toList();
        final List<Field> annotatedFields = getAnnotatedFields(clazz, dataList, readOnly);
        final Sheet sheet = workbook.createSheet();
        final Map<String, CellStyle> styleCache = new HashMap<>();

        sheet.protectSheet("");
        if (sheet instanceof final SXSSFSheet sxSheet) {
            sxSheet.trackAllColumnsForAutoSizing();
        }

        setSheetName(messageSource, locale, workbook, sheet, clazz, overrideSheetName);
        addHeaderRow(messageSource, locale, sheet, annotatedFields, styleCache);
        writeRows(sheet, dataList.iterator(), annotatedFields, styleCache);
        finalizeSheet(sheet, annotatedFields, styleCache, readOnly);
    }

    private void setSheetName(final MessageSource messageSource, final Locale locale, final Workbook workbook, final Sheet sheet, final Class<?> clazz, @Nullable final String overrideSheetName) {
        final String sheetName;
        if (hasText(overrideSheetName)) {
            sheetName = messageSource.getMessage(overrideSheetName, null, overrideSheetName, locale);
        } else if (clazz.isAnnotationPresent(ExcelSheet.class)) {
            final String name = clazz.getAnnotation(ExcelSheet.class).name();
            sheetName = messageSource.getMessage(name, null, name, locale);
        } else {
            sheetName = parseCamelCase(clazz.getSimpleName());
        }
        workbook.setSheetName(workbook.getSheetIndex(sheet), sheetName);
    }

    private void addHeaderRow(final MessageSource messageSource, final Locale locale, final Sheet sheet, final List<Field> annotatedFields, final Map<String, CellStyle> styleCache) {
        final Row row = sheet.createRow(0);
        final CellStyle style = getProtectionStyle(sheet.getWorkbook(), styleCache, true);

        for (int position = 0; position < annotatedFields.size(); position++) {
            final Field field = annotatedFields.get(position);
            final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);

            String header = excelCell.header();
            header = hasText(header) ? messageSource.getMessage(header, null, header, locale) : parseCamelCase(field.getName());

            final Cell cell = row.createCell(position);
            cell.setCellValue(header);
            cell.setCellStyle(style);
            // Rough estimate for width: header length + padding
            sheet.setColumnWidth(position, ((header.length() + 3) * 256) + 200);
        }
    }

    private CellStyle getProtectionStyle(final Workbook workbook, final Map<String, CellStyle> styleCache, final boolean locked) {
        return styleCache.computeIfAbsent(String.format("locked-%b", locked), _ -> {
            final CellStyle style = workbook.createCellStyle();

            style.setLocked(locked);

            return style;
        });
    }

    private void writeRows(final Sheet sheet, final Iterator<?> iterator, final List<Field> annotatedFields, final Map<String, CellStyle> styleCache) {
        int rowNum = 1;

        while (iterator.hasNext()) {
            final Row row = sheet.createRow(rowNum++);
            final Object item = iterator.next();

            for (int position = 0; position < annotatedFields.size(); position++) {
                final Field field = annotatedFields.get(position);
                final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);

                try {
                    final Cell cell = row.createCell(position);

                    applyCellStyling(cell, excelCell, styleCache);

                    final Object value = field.get(item);

                    if (hasText(excelCell.transform()) && value != null) {
                        final Object transformed = PARSER.parseRaw(excelCell.transform()).getValue(value);
                        if (transformed != null) {
                            CellTypedWriterFactory.getTypedWriter(transformed.getClass()).accept(cell, transformed);
                        }
                    } else if (value != null) {
                        CellTypedWriterFactory.getTypedWriter(value.getClass()).accept(cell, value);
                    }
                } catch (final Exception e) {
                    log.warn("Could not write row {} cell {}: {}", row.getRowNum() + 1, position, e.getMessage());
                }
            }
        }
    }

    private void applyCellStyling(final Cell cell, final ExcelCell excelCell, final Map<String, CellStyle> styleCache) {
        final String cacheKey = String.format("upd-%b-man-%b", excelCell.updatable(), excelCell.mandatory());

        final CellStyle style = styleCache.computeIfAbsent(cacheKey, key -> {
            final Workbook workbook = cell.getSheet().getWorkbook();
            final CellStyle newStyle = workbook.createCellStyle();

            newStyle.setLocked(!excelCell.updatable());

            final IndexedColors color;
            if (excelCell.updatable() && excelCell.mandatory()) {
                color = IndexedColors.GREEN;
            } else if (excelCell.updatable()) {
                color = IndexedColors.LIGHT_GREEN;
            } else if (excelCell.mandatory()) {
                color = IndexedColors.BRIGHT_GREEN;
            } else {
                color = IndexedColors.DARK_RED;
            }

            newStyle.setBorderTop(BorderStyle.THIN);
            newStyle.setBorderBottom(BorderStyle.THIN);
            newStyle.setBorderLeft(BorderStyle.THIN);
            newStyle.setBorderRight(BorderStyle.THIN);
            newStyle.setTopBorderColor(color.getIndex());
            newStyle.setBottomBorderColor(color.getIndex());
            newStyle.setLeftBorderColor(color.getIndex());
            newStyle.setRightBorderColor(color.getIndex());

            return newStyle;
        });

        cell.setCellStyle(style);
    }

    private void finalizeSheet(final Sheet sheet, final List<Field> annotatedFields, final Map<String, CellStyle> styleCache, final boolean readOnly) {
        final int lastColumn = annotatedFields.size();

        for (int position = 0; position < lastColumn; position++) {
            final int headerWidth = sheet.getColumnWidth(position);

            sheet.autoSizeColumn(position, false);

            if (sheet.getColumnWidth(position) < headerWidth) {
                sheet.setColumnWidth(position, headerWidth);
            }
        }

        if (readOnly) {
            setTabColor(sheet);
        } else {
            final CellStyle style = getProtectionStyle(sheet.getWorkbook(), styleCache, false);

            for (int position = 0; position < lastColumn; position++) {
                sheet.setDefaultColumnStyle(position, style);
            }
        }

        sheet.createFreezePane(0, 1);

        if (lastColumn > 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, lastColumn - 1));
            unlockAutoFilter(sheet);
        }
    }

    private void setTabColor(final Sheet sheet) {
        final byte[] red = DefaultIndexedColorMap.getDefaultRGB(IndexedColors.RED.getIndex());

        if (sheet instanceof final SXSSFSheet sxssfSheet) {
            sxssfSheet.setTabColor(new XSSFColor(red));
        } else if (sheet instanceof final XSSFSheet xssfSheet) {
            xssfSheet.setTabColor(new XSSFColor(red));
        }
    }

    private void unlockAutoFilter(final Sheet sheet) {
        if (sheet instanceof final SXSSFSheet sxssfSheet) {
            sxssfSheet.lockAutoFilter(false);
        } else if (sheet instanceof final XSSFSheet xssfSheet) {
            xssfSheet.lockAutoFilter(false);
        }
    }

    private <T> List<Field> getAnnotatedFields(final Class<T> clazz, final List<T> data, final boolean readOnly) {
        final List<Field> fields = Arrays.stream(FieldUtils.getAllFields(clazz))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .filter(field -> !(readOnly && field.getName().equals("action")))
                .peek(field -> field.setAccessible(true))
                .filter(field -> {
                    final ExcelCell annotation = field.getAnnotation(ExcelCell.class);

                    if (annotation.ignoreIfNull()) {
                        return data.stream().anyMatch(item -> {
                            try {
                                return field.get(item) != null;
                            } catch (final IllegalAccessException e) {
                                return false;
                            }
                        });
                    }
                    return true;
                })
                .toList();

        final int maxPosition = ImpexUtil.determinePositionBeforeAuditableFields(fields);

        return fields.stream()
                .sorted(Comparator.comparingInt(field -> ImpexUtil.determinePosition(field, maxPosition, readOnly)))
                .toList();
    }
}