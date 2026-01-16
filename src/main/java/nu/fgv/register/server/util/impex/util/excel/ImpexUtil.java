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

package nu.fgv.register.server.util.impex.util.excel;

import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.util.impex.model.HasImpexAction;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class ImpexUtil {

    private ImpexUtil() {
    }

    public static int determinePositionBeforeAuditableFields(final List<Field> annotatedFields) {
        return annotatedFields.stream()
                .filter(f -> !f.getDeclaringClass().equals(AbstractAuditableDto.class))
                .map(f -> f.getAnnotation(ExcelCell.class).position())
                .mapToInt(v -> v)
                .max()
                .orElse(-1) + 1;
    }

    public static int determinePosition(final Field field, final int maxPosition, final boolean readOnly) {
        final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);

        if (readOnly) {
            if (field.getName().equals("action")) {
                return -1;
            }

            int pos = excelCell.position();
            if (field.getDeclaringClass().equals(AbstractAuditableDto.class) || field.getDeclaringClass().equals(AbstractAuditableImpexDto.class)) {
                pos += maxPosition;
            }

            return pos > 0 ? pos - 1 : 0;
        }

        if (field.getDeclaringClass().equals(AbstractAuditableDto.class) || field.getDeclaringClass().equals(AbstractAuditableImpexDto.class)) {
            return excelCell.position() + maxPosition;
        }

        return excelCell.position();
    }

    public static boolean isMarkedForDeletion(final Cell cell) {
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().toLowerCase().endsWith("d");
    }

    public static boolean isMarkedForCreation(final Cell cell) {
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().toLowerCase().endsWith("n");
    }

    public static boolean isMarkedForUpdate(final Cell cell) {
        return !isMarkedForDeletion(cell) && !isMarkedForCreation(cell);
    }

}
