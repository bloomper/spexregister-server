package nu.fgv.register.server.util.export;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.function.BiConsumer;

@Slf4j
class CellWriter extends FieldAccessor {

    public CellWriter(final Field field) {
        super(field);
    }

    public BiConsumer<Cell, Object> intWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(this.getInt(obj));
    };

    public BiConsumer<Cell, Object> shortWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(this.getShort(obj));
    };

    public BiConsumer<Cell, Object> longWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(this.getLong(obj));
    };

    public BiConsumer<Cell, Object> doubleWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(this.getDouble(obj));
    };

    public BiConsumer<Cell, Object> floatWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(this.getFloat(obj));
    };

    public BiConsumer<Cell, Object> byteWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(this.getByte(obj));
    };

    public BiConsumer<Cell, Object> charWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(String.valueOf(this.getChar(obj)));
    };

    public BiConsumer<Cell, Object> booleanWriter = (Cell cell, Object obj) -> {
        cell.setCellValue(this.getBoolean(obj));
    };

    public BiConsumer<Cell, Object> utilDateWriter = (Cell cell, Object obj) -> {
        Date value = null;
        try {
            value = (Date) field.get(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException | ClassCastException e) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public BiConsumer<Cell, Object> sqlDateWriter = (Cell cell, Object obj) -> {
        Date value = null;
        try {
            value = new Date(((java.sql.Date) field.get(obj)).getTime());
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException | ClassCastException e) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public BiConsumer<Cell, Object> calendarWriter = (Cell cell, Object obj) -> {
        Date value = null;
        try {
            value = ((Calendar) field.get(obj)).getTime();
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException | ClassCastException e) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public BiConsumer<Cell, Object> stringWriter = (Cell cell, Object obj) -> {
        obj = this.getObject(obj);
        if (obj != null) {
            cell.setCellValue(obj.toString());
        }
    };
}
