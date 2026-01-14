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
import nu.fgv.register.server.util.impex.exporting.FieldAccessor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.function.BiConsumer;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
class CellWriter extends FieldAccessor {

    public CellWriter(final Field field) {
        super(field);
    }

    public final BiConsumer<Cell, Object> intWriter = (final Cell cell, final Object obj) -> cell.setCellValue(getInt(obj));

    public final BiConsumer<Cell, Object> shortWriter = (final Cell cell, final Object obj) -> cell.setCellValue(getShort(obj));

    public final BiConsumer<Cell, Object> longWriter = (final Cell cell, final Object obj) -> cell.setCellValue(getLong(obj));

    public final BiConsumer<Cell, Object> doubleWriter = (final Cell cell, final Object obj) -> cell.setCellValue(getDouble(obj));

    public final BiConsumer<Cell, Object> floatWriter = (final Cell cell, final Object obj) -> cell.setCellValue(getFloat(obj));

    public final BiConsumer<Cell, Object> byteWriter = (final Cell cell, final Object obj) -> cell.setCellValue(getByte(obj));

    public final BiConsumer<Cell, Object> charWriter = (final Cell cell, final Object obj) -> cell.setCellValue(String.valueOf(getChar(obj)));

    public final BiConsumer<Cell, Object> booleanWriter = (final Cell cell, final Object obj) -> cell.setCellValue(getBoolean(obj));

    public final BiConsumer<Cell, Object> utilDateWriter = (final Cell cell, final Object obj) -> {
        Date value = null;

        try {
            value = (Date) field.get(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException | ClassCastException _) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public final BiConsumer<Cell, Object> sqlDateWriter = (final Cell cell, final Object obj) -> {
        Date value = null;

        try {
            value = new Date(((java.sql.Date) field.get(obj)).getTime());
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException | ClassCastException _) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public final BiConsumer<Cell, Object> calendarWriter = (final Cell cell, final Object obj) -> {
        Date value = null;

        try {
            value = ((Calendar) field.get(obj)).getTime();
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException | ClassCastException _) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public final BiConsumer<Cell, Object> stringWriter = (final Cell cell, final Object obj) -> {
        final Object o = getObject(obj);

        if (o != null) {
            cell.setCellValue(o.toString());
        }
    };
}
