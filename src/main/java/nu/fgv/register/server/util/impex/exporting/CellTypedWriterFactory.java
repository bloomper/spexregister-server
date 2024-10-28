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

package nu.fgv.register.server.util.impex.exporting;

import org.apache.poi.ss.usermodel.Cell;
import org.springframework.lang.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.function.BiConsumer;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class CellTypedWriterFactory {

    private CellTypedWriterFactory() {
    }

    public static BiConsumer<Cell, Object> getTypedWriter(@Nullable final Class<?> clazz) {
        final CellTypedWriter cellTypedWriter = new CellTypedWriter();

        if (clazz == Integer.class || clazz == int.class) {
            return cellTypedWriter.intWriter;
        } else if (clazz == Short.class || clazz == short.class) {
            return cellTypedWriter.shortWriter;
        } else if (clazz == Long.class || clazz == long.class) {
            return cellTypedWriter.longWriter;
        } else if (clazz == Double.class || clazz == double.class) {
            return cellTypedWriter.doubleWriter;
        } else if (clazz == Float.class || clazz == float.class) {
            return cellTypedWriter.floatWriter;
        } else if (clazz == Byte.class || clazz == byte.class) {
            return cellTypedWriter.byteWriter;
        } else if (clazz == Character.class || clazz == char.class) {
            return cellTypedWriter.charWriter;
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return cellTypedWriter.booleanWriter;
        } else if (clazz == Date.class) {
            return cellTypedWriter.utilDateWriter;
        } else if (clazz == Calendar.class) {
            return cellTypedWriter.calendarWriter;
        } else if (clazz == java.sql.Date.class) {
            return cellTypedWriter.sqlDateWriter;
        } else {
            return cellTypedWriter.stringWriter;
        }
    }

}
