package nu.fgv.register.server.util.impex.exporting;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;

import java.util.Calendar;
import java.util.Date;
import java.util.function.BiConsumer;

@Slf4j
class CellTypedWriter {

    public final BiConsumer<Cell, Object> intWriter = (Cell cell, Object obj) -> cell.setCellValue((int) obj);

    public final BiConsumer<Cell, Object> shortWriter = (Cell cell, Object obj) -> cell.setCellValue((short)obj);

    public final BiConsumer<Cell, Object> longWriter = (Cell cell, Object obj) -> cell.setCellValue((long) obj);

    public final BiConsumer<Cell, Object> doubleWriter = (Cell cell, Object obj) -> cell.setCellValue((double) obj);

    public final BiConsumer<Cell, Object> floatWriter = (Cell cell, Object obj) -> cell.setCellValue((float) obj);

    public final BiConsumer<Cell, Object> byteWriter = (Cell cell, Object obj) -> cell.setCellValue((byte) obj);

    public final BiConsumer<Cell, Object> charWriter = (Cell cell, Object obj) -> cell.setCellValue(String.valueOf((char) obj));

    public final BiConsumer<Cell, Object> booleanWriter = (Cell cell, Object obj) -> cell.setCellValue((boolean) obj);

    public final BiConsumer<Cell, Object> utilDateWriter = (Cell cell, Object obj) -> {
        Date value = null;
        try {
            value = (Date) obj;
        } catch (final IllegalArgumentException | NullPointerException | ClassCastException e) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public final BiConsumer<Cell, Object> sqlDateWriter = (Cell cell, Object obj) -> {
        Date value = null;
        try {
            value = new Date(((java.sql.Date) obj).getTime());
        } catch (final IllegalArgumentException | NullPointerException | ClassCastException e) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public final BiConsumer<Cell, Object> calendarWriter = (Cell cell, Object obj) -> {
        Date value = null;
        try {
            value = ((Calendar) obj).getTime();
        } catch (final IllegalArgumentException | NullPointerException | ClassCastException e) {
            log.warn("Could not write to cell, defaulting to ERROR");
            cell.setCellErrorValue(FormulaError.VALUE.getCode());
        }
        cell.setCellValue(value);
    };

    public final BiConsumer<Cell, Object> stringWriter = (Cell cell, Object obj) -> {
        if (obj != null) {
            cell.setCellValue(obj.toString());
        }
    };
}
