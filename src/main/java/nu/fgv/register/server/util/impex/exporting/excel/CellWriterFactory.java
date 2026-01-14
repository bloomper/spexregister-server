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

package nu.fgv.register.server.util.impex.exporting.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.function.BiConsumer;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
class CellWriterFactory {

    private final WorkbookContainer container;

    public CellWriterFactory(final WorkbookContainer container) {
        this.container = container;
    }

    public BiConsumer<Cell, Object> getFieldWriter(final Field field) {
        final Class<?> fieldClass = field.getType();
        final CellWriter cellWriter = new CellWriter(field);

        if (fieldClass == Integer.class || fieldClass == int.class) {
            return cellWriter.intWriter;
        } else if (fieldClass == Short.class || fieldClass == short.class) {
            return cellWriter.shortWriter;
        } else if (fieldClass == Long.class || fieldClass == long.class) {
            return cellWriter.longWriter;
        } else if (fieldClass == Double.class || fieldClass == double.class) {
            return cellWriter.doubleWriter;
        } else if (fieldClass == Float.class || fieldClass == float.class) {
            return cellWriter.floatWriter;
        } else if (fieldClass == Byte.class || fieldClass == byte.class) {
            return cellWriter.byteWriter;
        } else if (fieldClass == Character.class || fieldClass == char.class) {
            return cellWriter.charWriter;
        } else if (fieldClass == Boolean.class || fieldClass == boolean.class) {
            return cellWriter.booleanWriter;
        } else if (fieldClass == Date.class) {
            return cellWriter.utilDateWriter;
        } else if (fieldClass == Calendar.class) {
            return cellWriter.calendarWriter;
        } else if (fieldClass == java.sql.Date.class) {
            return cellWriter.sqlDateWriter;
        } else {
            return cellWriter.stringWriter;
        }
    }

    public BiConsumer<Cell, String> getHeaderWriter() {
        final Workbook workbook = this.container.getWorkbook();
        final Font font = workbook.createFont();

        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());

        final CellStyle style = workbook.createCellStyle();

        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBottomBorderColor(IndexedColors.BLUE1.getIndex());
        style.setFont(font);

        return (final Cell cell, final String header) -> {
            cell.setCellValue(header);
            cell.setCellStyle(style);
        };
    }

}
