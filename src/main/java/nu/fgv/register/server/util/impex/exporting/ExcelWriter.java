package nu.fgv.register.server.util.impex.exporting;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.context.MessageSource;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePosition;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePositionBeforeAuditableFields;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.setCellBorders;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class ExcelWriter {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    final WorkbookContainer workbookContainer = new WorkbookContainer();

    public <T> Optional<Sheet> createSheet(final MessageSource messageSource, final Locale locale, final Workbook workbook, final List<T> data) {
        return createSheet(messageSource, locale, workbook, data, null);
    }

    public <T> Optional<Sheet> createSheet(final MessageSource messageSource, final Locale locale, final Workbook workbook, final List<T> data, final String overrideSheetName) {
        workbookContainer.setMessageSource(messageSource);
        workbookContainer.setLocale(locale);
        workbookContainer.setWorkbook(workbook);

        return data != null && !data.isEmpty() ?
                Optional.of(initialize
                        .andThen(generateSheetName)
                        .andThen(addColumns)
                        .andThen(writeData)
                        .andThen(autoSizeColumns)
                        .andThen(freezePane)
                        .andThen(attachFilters)
                        .apply(data, overrideSheetName)
                        .getSheet()) :
                Optional.empty();
    }

    private final BiFunction<List<?>, String, SheetContainer> initialize = (final List<?> data, final String overrideSheetName) -> {
        final SheetContainer sheetContainer = new SheetContainer();

        sheetContainer.setOverrideSheetName(overrideSheetName);
        sheetContainer.setSheet(workbookContainer.getWorkbook().createSheet());
        sheetContainer.setData(data);

        final Class<?> clazz = data.getFirst().getClass();
        sheetContainer.setAnnotatedFields(
                Arrays.stream(FieldUtils.getAllFields(clazz))
                        .filter(field -> {
                            field.setAccessible(true); // NOSONAR
                            return field.isAnnotationPresent(ExcelCell.class);
                        }).toList()
        );

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> generateSheetName = (final SheetContainer sheetContainer) -> {
        final Workbook workbook = workbookContainer.getWorkbook();
        final Sheet sheet = sheetContainer.getSheet();
        final String sheetName;
        final Class<?> clazz = sheetContainer.getData().getFirst().getClass();

        if (hasText(sheetContainer.getOverrideSheetName())) {
            sheetName = sheetContainer.getOverrideSheetName();
        } else if (clazz.isAnnotationPresent(ExcelSheet.class)) {
            final String name = clazz.getAnnotation(ExcelSheet.class).name();

            sheetName = workbookContainer.getMessageSource().getMessage(name, null, name, workbookContainer.getLocale());
        } else {
            sheetName = parseCamelCase(clazz.getSimpleName());
        }

        workbook.setSheetName(workbook.getSheetIndex(sheet), sheetName);

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> addColumns = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final Row row = sheet.createRow(0);

        try {
            final BiConsumer<Cell, String> columnWriter = workbookContainer.getWriterFactory().getHeaderWriter();
            final ObjIntConsumer<String> addColumn = (final String header, final int position) -> {
                final Cell cell = row.createCell(position);
                columnWriter.accept(cell, header);
                sheet.setColumnWidth(position, ((header.length() + 3) * 256) + 200);
            };
            final int maxPosition = determinePositionBeforeAuditableFields(sheetContainer.getAnnotatedFields());

            sheetContainer.getAnnotatedFields().forEach(field -> {
                final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
                final int position = determinePosition(field, maxPosition);
                String header = excelCell.header();

                if (!hasText(header)) {
                    header = parseCamelCase(field.getName());
                } else {
                    header = workbookContainer.getMessageSource().getMessage(header, null, header, workbookContainer.getLocale());
                }

                addColumn.accept(header, position);
            });
        } catch (final Exception e) {
            log.error("Could not add columns to sheet {}", sheet.getSheetName(), e);
        }
        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> writeData = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final List<?> data = sheetContainer.getData();

        try {
            final Map<Field, BiConsumer<Cell, Object>> fieldWriters = new HashMap<>();

            sheetContainer.getAnnotatedFields().forEach(field -> {
                final BiConsumer<Cell, Object> cellWriter = workbookContainer.getWriterFactory().getFieldWriter(field);

                fieldWriters.put(field, cellWriter);
            });

            IntStream.range(0, data.size()).forEach(rowNum -> {
                final Row row = sheet.createRow(rowNum + 1);
                final Object value = data.get(rowNum);
                final int maxPosition = determinePositionBeforeAuditableFields(sheetContainer.getAnnotatedFields());

                sheetContainer.getAnnotatedFields().forEach(field -> {
                    final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
                    final int position = determinePosition(field, maxPosition);

                    try {
                        final Cell cell;

                        if (hasText(excelCell.transform()) && field.get(value) != null) {
                            final SpelExpression spelExpression = PARSER.parseRaw(excelCell.transform());
                            final Object transformedValue = spelExpression.getValue(field.get(value));
                            final Class<?> transformedClazz = spelExpression.getValueType(field.get(value));
                            cell = row.createCell(position);

                            CellTypedWriterFactory.getTypedWriter(transformedClazz).accept(cell, transformedValue);
                        } else {
                            cell = row.createCell(position);
                            fieldWriters.get(field).accept(cell, value);
                        }

                        if (excelCell.updatable() && excelCell.mandatory()) {
                            setCellBorders(cell, BorderStyle.THIN, IndexedColors.GREEN);
                        } else if (excelCell.updatable()) {
                            setCellBorders(cell, BorderStyle.THIN, IndexedColors.LIGHT_GREEN);
                        } else if (excelCell.mandatory()) {
                            setCellBorders(cell, BorderStyle.THIN, IndexedColors.BRIGHT_GREEN);
                        } else {
                            setCellBorders(cell, BorderStyle.THIN, IndexedColors.DARK_RED);
                        }
                    } catch (final Exception e) {
                        log.warn("Could not write data to row {} cell {} of sheet {}", rowNum + 1, position, sheet.getSheetName(), e);
                    }
                });
            });
        } catch (Exception e) {
            log.error("Could not write data to sheet {}", sheet.getSheetName(), e);
        }
        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> autoSizeColumns = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final int lastColumn = sheet.getRow(sheet.getLastRowNum()).getLastCellNum();

        for (int column = 0; column < lastColumn; column++) {
            sheet.autoSizeColumn(column, false);
        }

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> freezePane = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();

        sheet.createFreezePane(0, 1);

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> attachFilters = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final int lastColumn = sheet.getRow(sheet.getLastRowNum()).getLastCellNum() - 1;

        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, lastColumn));

        return sheetContainer;
    };

}
