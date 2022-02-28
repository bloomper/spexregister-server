package nu.fgv.register.server.util.export;

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

        return (Cell cell, String header) -> {
            cell.setCellValue(header);
            cell.setCellStyle(style);
        };
    }

}
