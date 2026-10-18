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

package nu.fgv.register.server.impex.exporting.excel;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import nu.fgv.register.server.impex.importing.ImportSpec;
import nu.fgv.register.server.impex.importing.excel.ExcelReader;
import nu.fgv.register.server.impex.importing.excel.ExcelValidator;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.tag.TagImpexDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class ExcelWriterTest {

    private static final Locale LOCALE = Locale.ENGLISH;

    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = validatorFactory.getValidator();

    ExcelWriterTest() {
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.addMessage("tag.impex.sheetName", LOCALE, "Tags");
        messageSource.addMessage("tag.impex.id.columnName", LOCALE, "Id");
        messageSource.addMessage("tag.impex.name.columnName", LOCALE, "Name");
        messageSource.addMessage("common.impex.action.columnName", LOCALE, "Action");
    }

    @AfterEach
    void tearDown() throws Exception {
        workbook.close();
        validatorFactory.close();
    }

    @Test
    void should_write_a_sheet_that_validates_and_reads_back() {
        write(List.of(tag(1L, "Founder"), tag(2L, "Honorary member")), false);

        final ImportResultDto result = new ExcelValidator(new ExcelReader(messageSource))
                .validateSheet(messageSource, LOCALE, workbook, "Tags", ImportSpec.builder().clazz(TagImpexDto.class).build(), validator);
        final List<TagImpexDto> read = new ExcelReader(messageSource).read(workbook.getSheet("Tags"), TagImpexDto.class, LOCALE);

        assertThat(result.isSuccess(), is(true));
        assertThat(read.stream().map(TagImpexDto::getId).toList(), contains(1L, 2L));
        assertThat(read.stream().map(TagImpexDto::getName).toList(), contains("Founder", "Honorary member"));
    }

    @Test
    void should_order_columns_by_position_with_the_audit_fields_last() {
        write(List.of(tag(1L, "Founder")), false);

        final List<String> headers = headers(workbook.getSheet("Tags"));

        assertThat(headers.subList(0, 3), contains("Action", "Id", "Name"));
        assertThat(headers.get(3), startsWith("common.impex.createdBy"));
    }

    @Test
    void should_leave_out_the_action_column_when_read_only() {
        write(List.of(tag(1L, "Founder")), true);

        final Sheet sheet = workbook.getSheet("Tags");

        assertThat(headers(sheet).subList(0, 2), contains("Id", "Name"));
        assertThat(headers(sheet), not(hasItem("Action")));
        assertThat(sheet.getRow(1).getCell(1).getStringCellValue(), is(equalTo("Founder")));
    }

    @Test
    void should_report_a_row_failing_bean_validation() {
        write(List.of(tag(1L, "Founder"), tag(2L, "")), false);

        final ImportResultDto result = new ExcelValidator(new ExcelReader(messageSource))
                .validateSheet(messageSource, LOCALE, workbook, "Tags", ImportSpec.builder().clazz(TagImpexDto.class).build(), validator);

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getMessages().size(), is(equalTo(1)));
        assertThat(result.getMessages().getFirst(), startsWith("Row 3:"));
    }

    @Test
    void should_report_a_missing_mandatory_column() {
        write(List.of(tag(1L, "Founder")), false);
        workbook.getSheet("Tags").getRow(0).getCell(2).setCellValue("Renamed");

        final ImportResultDto result = new ExcelValidator(new ExcelReader(messageSource))
                .validateSheet(messageSource, LOCALE, workbook, "Tags", ImportSpec.builder().clazz(TagImpexDto.class).build(), validator);

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getMessages(), contains("impex.import.validation.columnMismatch"));
    }

    @Test
    void should_report_a_missing_sheet() {
        final ImportResultDto result = new ExcelValidator(new ExcelReader(messageSource))
                .validateSheet(messageSource, LOCALE, workbook, "Tags", ImportSpec.builder().clazz(TagImpexDto.class).build(), validator);

        assertThat(result.isSuccess(), is(false));
        assertThat(result.getMessages(), contains("impex.import.validation.missingSheet"));
    }

    @Test
    void should_report_a_row_whose_entry_does_not_exist() {
        write(List.of(tag(1L, "Founder"), tag(99L, "Ghost")), false);

        final ImportSpec spec = ImportSpec.builder()
                .clazz(TagImpexDto.class)
                .existenceCheckers(Map.of("id", id -> Long.valueOf(1L).equals(id)))
                .build();
        final ImportResultDto result = new ExcelValidator(new ExcelReader(messageSource))
                .validateSheet(messageSource, LOCALE, workbook, "Tags", spec, validator);

        assertThat(result.getMessages(), contains("Row 3: impex.import.validation.entryDoesNotExist"));
    }

    @Test
    void should_write_cells_by_value_type() {
        final Row row = workbook.createSheet().createRow(0);

        assertThat(written(row, 0, 42).getNumericCellValue(), is(equalTo(42d)));
        assertThat(written(row, 1, 4_000_000_000L).getNumericCellValue(), is(equalTo(4e9)));
        assertThat(written(row, 2, true).getBooleanCellValue(), is(true));
        assertThat(written(row, 3, 'x').getStringCellValue(), is(equalTo("x")));
        assertThat(written(row, 4, new Date(0)).getDateCellValue(), is(equalTo(new Date(0))));
        assertThat(written(row, 5, new java.sql.Date(0)).getDateCellValue(), is(equalTo(new Date(0))));

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(0));
        assertThat(written(row, 6, calendar).getDateCellValue(), is(equalTo(new Date(0))));
        assertThat(written(row, 7, Locale.ENGLISH).getStringCellValue(), is(equalTo("en")));
        assertThat(written(row, 8, new java.sql.Timestamp(0)).getDateCellValue(), is(equalTo(new Date(0))));
    }

    private Cell written(final Row row, final int column, final Object value) {
        final Cell cell = row.createCell(column);
        final BiConsumer<Cell, Object> writer = CellTypedWriterFactory.getTypedWriter(value.getClass());

        writer.accept(cell, value);
        return cell;
    }

    private void write(final List<TagImpexDto> tags, final boolean readOnly) {
        new ExcelWriter().createSheet(messageSource, LOCALE, workbook, tags, null, TagImpexDto.class, readOnly);
    }

    private static List<String> headers(final Sheet sheet) {
        final List<String> headers = new ArrayList<>();

        sheet.getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));
        return headers;
    }

    private static TagImpexDto tag(final Long id, final String name) {
        final TagImpexDto dto = new TagImpexDto();

        dto.setId(id);
        dto.setName(name);
        return dto;
    }
}
