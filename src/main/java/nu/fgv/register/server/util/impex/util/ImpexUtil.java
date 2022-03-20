package nu.fgv.register.server.util.impex.util;

import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.ExcelCell;

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

}
