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

package nu.fgv.register.server.impex.util.excel;

import nu.fgv.register.server.impex.model.excel.ExcelCell;
import nu.fgv.register.server.util.AbstractAuditableDto;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Column positions of a row type.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public class ImpexUtil {

    private ImpexUtil() {
    }

    /**
     * @param annotatedFields the {@link nu.fgv.register.server.impex.model.excel.ExcelCell} fields of a row type
     * @return the first position after its own columns, where the audit columns start
     */
    public static int determinePositionBeforeAuditableFields(final List<Field> annotatedFields) {
        return annotatedFields.stream()
                .filter(f -> !f.getDeclaringClass().equals(AbstractAuditableDto.class))
                .map(f -> f.getAnnotation(ExcelCell.class).position())
                .mapToInt(v -> v)
                .max()
                .orElse(-1) + 1;
    }

    /**
     * @param field       an {@link nu.fgv.register.server.impex.model.excel.ExcelCell} field
     * @param maxPosition the first position of the audit columns
     * @param readOnly    whether the sheet is read-only, without the action column
     * @return the field's column, or -1 when it is left out
     */
    public static int determinePosition(final Field field, final int maxPosition, final boolean readOnly) {
        final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
        final String fieldName = field.getName();

        if (readOnly) {
            if (fieldName.equals("action")) {
                return -1;
            }

            int pos = excelCell.position();
            if (field.getDeclaringClass().equals(AbstractAuditableDto.class)) {
                pos += maxPosition;
            }

            return pos > 0 ? pos - 1 : 0;
        }

        if (field.getDeclaringClass().equals(AbstractAuditableDto.class)) {
            return excelCell.position() + maxPosition;
        }

        return excelCell.position();
    }

}
