package nu.fgv.register.server.util.export;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.export.model.ExcelCell;
import nu.fgv.register.server.util.export.model.ExcelSheet;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
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
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
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
                Optional.of(createSheet
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

    private final BiFunction<List<?>, String, SheetContainer> createSheet = (final List<?> data, final String overrideSheetName) -> {
        final SheetContainer sheetContainer = new SheetContainer();
        sheetContainer.setOverrideSheetName(overrideSheetName);
        sheetContainer.setSheet(workbookContainer.getWorkbook().createSheet());
        sheetContainer.setData(data);
        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> generateSheetName = (final SheetContainer sheetContainer) -> {
        final Workbook workbook = workbookContainer.getWorkbook();
        final Sheet sheet = sheetContainer.getSheet();
        final String sheetName;

        final Class<?> clazz = sheetContainer.getData().get(0).getClass();

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

    private final Function<SheetContainer, SheetContainer> addColumns = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final List<?> data = sheetContainer.getData();
        final Row row = sheet.createRow(0);

        try {
            final Class<?> clazz = data.get(0).getClass();
            final Field[] fields = FieldUtils.getAllFields(clazz);
            final List<Field> annotatedFields = Arrays.stream(fields).filter(field -> {
                field.setAccessible(true);
                return field.isAnnotationPresent(ExcelCell.class);
            }).toList();
            final BiConsumer<Cell, String> columnWriter = workbookContainer.getWriterFactory().getHeaderWriter();
            final BiConsumer<String, Integer> addColumn = (final String header, final Integer position) -> {
                final Cell cell = row.createCell(position);
                columnWriter.accept(cell, header);
                sheet.setColumnWidth(position, ((header.length() + 3) * 256) + 200);
            };

            final int maxPosition = determineMaxPosition(annotatedFields);
            annotatedFields.forEach(field -> {
                final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
                String header = excelCell.header();
                final int position = determinePosition(field, maxPosition);

                if (!hasText(header)) {
                    header = parseCamelCase(field.getName());
                } else {
                    header = workbookContainer.getMessageSource().getMessage(header, null, header, workbookContainer.getLocale());
                }

                addColumn.accept(header, position);
            });
        } catch (final Exception e) {
            log.error(String.format("Could not add columns to sheet %s", sheet.getSheetName()), e);
        }
        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> writeData = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final List<?> data = sheetContainer.getData();

        try {
            final Class<?> clazz = data.get(0).getClass();
            final Field[] fields = FieldUtils.getAllFields(clazz);
            final List<Field> annotatedFields = Arrays.stream(fields)
                    .filter(field -> {
                        field.setAccessible(true);
                        return field.isAnnotationPresent(ExcelCell.class);
                    }).toList();
            final Map<Field, BiConsumer<Cell, Object>> fieldWriters = new HashMap<>();
            annotatedFields.forEach(field -> {
                final BiConsumer<Cell, Object> cellWriter = workbookContainer.getWriterFactory().getFieldWriter(field);
                fieldWriters.put(field, cellWriter);
            });

            IntStream.range(0, data.size()).forEach(rowNum -> {
                final Row row = sheet.createRow(rowNum + 1);
                final Object value = data.get(rowNum);

                final int maxPosition = determineMaxPosition(annotatedFields);
                annotatedFields.forEach(field -> {
                    final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
                    final int position = determinePosition(field, maxPosition);

                    try {
                        if (hasText(excelCell.transform()) && field.get(value) != null) {
                            final SpelExpression spelExpression = PARSER.parseRaw(excelCell.transform());
                            final Object transformedValue = spelExpression.getValue(field.get(value));
                            final Class<?> transformedClazz = spelExpression.getValueType(field.get(value));
                            final Cell cell = row.createCell(position);
                            CellTypedWriterFactory.getTypedWriter(transformedClazz).accept(cell, transformedValue);
                        } else {
                            final Cell cell = row.createCell(position);
                            fieldWriters.get(field).accept(cell, value);
                        }
                    } catch (final Exception e) {
                        log.warn(String.format("Could not write data to row %s cell %s of sheet %s", rowNum + 1, position, sheet.getSheetName()), e);
                    }
                });
            });
        } catch (Exception e) {
            log.error(String.format("Could not write data to sheet %s", sheet.getSheetName()), e);
        }
        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> autoSizeColumns = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final int lastColumn = sheet.getRow(sheet.getLastRowNum()).getLastCellNum();

        for (int column = 0; column < lastColumn; column++) {
            sheet.autoSizeColumn(column, false);
        }

        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> freezePane = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        sheet.createFreezePane(0, 1);
        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> attachFilters = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final int lastColumn = sheet.getRow(sheet.getLastRowNum()).getLastCellNum() - 1;
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, lastColumn));
        return sheetContainer;
    };

    private static int determineMaxPosition(final List<Field> annotatedFields) {
        final int maxPosition = annotatedFields.stream()
                .filter(f -> !f.getDeclaringClass().equals(AbstractAuditableDto.class))
                .map(f -> f.getAnnotation(ExcelCell.class).position())
                .mapToInt(v -> v)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("Could not determine max position"));
        return maxPosition + 1;
    }

    private static int determinePosition(final Field field, final int maxPosition) {
        final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
        return excelCell.position() + (field.getDeclaringClass().equals(AbstractAuditableDto.class) ? maxPosition : 0);
    }

    private static String parseCamelCase(final String camelCaseString) {
        if (camelCaseString == null) {
            return "";
        } else {
            return capitalize(String.join(" ", splitByCharacterTypeCamelCase(camelCaseString)));
        }
    }
}
