package nu.fgv.register.server.util.impex.exporting;

import org.apache.poi.ss.usermodel.Cell;

import java.util.Calendar;
import java.util.Date;
import java.util.function.BiConsumer;

class CellTypedWriterFactory {

    public static BiConsumer<Cell, Object> getTypedWriter(final Class<?> clazz) {
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
