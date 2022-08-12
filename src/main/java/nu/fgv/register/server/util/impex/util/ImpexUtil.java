package nu.fgv.register.server.util.impex.util;

import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.ExcelCell;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.lang.reflect.Field;
import java.util.List;

public class ImpexUtil {

    private ImpexUtil() {
    }

    public static int determinePositionBeforeAuditableFields(final List<Field> annotatedFields) {
        final int maxPosition = annotatedFields.stream()
                .filter(f -> !f.getDeclaringClass().equals(AbstractAuditableDto.class))
                .map(f -> f.getAnnotation(ExcelCell.class).position())
                .mapToInt(v -> v)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("Could not determine position before auditable fields"));
        return maxPosition + 1;
    }

    public static int determinePosition(final Field field, final int maxPosition) {
        final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
        return excelCell.position() + (field.getDeclaringClass().equals(AbstractAuditableDto.class) ? maxPosition : 0);
    }

    public static boolean isMarkedForDeletion(final Cell cell) {
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().toLowerCase().endsWith("d");
    }

    public static void setCellBorders(final Cell cell, final BorderStyle borderStyle, final IndexedColors color) {
        CellStyle cellStyle = cell.getCellStyle();
        if (cellStyle == null) {
            cellStyle = cell.getSheet().getWorkbook().createCellStyle();
        }
        cellStyle.setBorderTop(borderStyle);
        cellStyle.setTopBorderColor(color.getIndex());
        cellStyle.setBorderLeft(borderStyle);
        cellStyle.setLeftBorderColor(color.getIndex());
        cellStyle.setBorderRight(borderStyle);
        cellStyle.setRightBorderColor(color.getIndex());
        cellStyle.setBorderBottom(borderStyle);
        cellStyle.setBottomBorderColor(color.getIndex());

        cell.setCellStyle(cellStyle);
    }
}
