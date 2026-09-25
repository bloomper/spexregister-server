/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.impex.importing.excel;

import nu.fgv.register.server.impex.model.excel.ExcelCell;
import nu.fgv.register.server.tag.TagImpexDto;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class ExcelReaderTest {

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final Row row = workbook.createSheet().createRow(1);

    @AfterEach
    void tearDown() throws Exception {
        workbook.close();
    }

    @Test
    void should_read_clean_whole_numbers() {
        assertThat(ExcelReader.wholeNumberOf(text(0, " 12 ")), is(equalTo(12L)));
        assertThat(ExcelReader.wholeNumberOf(number(1, 12.0)), is(equalTo(12L)));
        assertThat(ExcelReader.wholeNumberOf(text(2, "")), is(nullValue()));
    }

    @Test
    void should_reject_anything_that_is_not_a_clean_whole_number() {
        final CellValueException exception = assertThrows(CellValueException.class, () -> ExcelReader.wholeNumberOf(text(0, "12 (was 3)")));

        assertThat(exception.getCode(), is(equalTo("impex.import.validation.notWholeNumber")));
        assertThrows(CellValueException.class, () -> ExcelReader.wholeNumberOf(text(1, "abc")));
        assertThrows(CellValueException.class, () -> ExcelReader.wholeNumberOf(number(2, 12.5)));
    }

    @Test
    void should_read_a_formula_by_its_result() {
        final Cell cell = row.createCell(0);

        cell.setCellFormula("6*2");
        workbook.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(cell);

        assertThat(ExcelReader.wholeNumberOf(cell), is(equalTo(12L)));
        assertThat(ExcelReader.stringValueOf(cell), is(equalTo("12")));
    }

    @Test
    void should_reject_an_unknown_action() {
        final List<Field> fields = Arrays.stream(FieldUtils.getAllFields(TagImpexDto.class))
                .filter(field -> field.isAnnotationPresent(ExcelCell.class))
                .toList();
        final Field action = fields.stream().filter(field -> field.getName().equals("action")).findFirst().orElseThrow();

        text(0, "DEL");

        final CellValueException exception = assertThrows(CellValueException.class,
                () -> new ExcelReader(new StaticMessageSource()).mapRowToDto(row, TagImpexDto.class, fields, fields.size(), Map.of(action, 0)));

        assertThat(exception.getCode(), is(equalTo("impex.import.validation.unknownAction")));
    }

    private Cell text(final int column, final String value) {
        final Cell cell = row.createCell(column);

        cell.setCellValue(value);
        return cell;
    }

    private Cell number(final int column, final double value) {
        final Cell cell = row.createCell(column);

        cell.setCellValue(value);
        return cell;
    }
}
