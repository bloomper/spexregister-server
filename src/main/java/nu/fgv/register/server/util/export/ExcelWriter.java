package nu.fgv.register.server.util.export;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.export.model.ExcelCell;
import nu.fgv.register.server.util.export.model.ExcelSheet;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class ExcelWriter {

    final WorkbookContainer workbookContainer = new WorkbookContainer();

    public <T> Optional<Sheet> createSheet(final Workbook workbook, final List<T> models) {
        workbookContainer.setWorkbook(workbook);
        return models != null && !models.isEmpty() ?
                Optional.of(createSheet
                        .andThen(generateSheetName)
                        .andThen(addColumns)
                        .andThen(writeData)
                        .andThen(autoSizeColumns)
                        .andThen(freezePane)
                        .andThen(attachFilters)
                        .apply(models)
                        .getSheet()) :
                Optional.empty();
    }

    private final Function<List<?>, SheetContainer> createSheet = (final List<?> models) -> {
        final SheetContainer sheetContainer = new SheetContainer();
        sheetContainer.setSheet(workbookContainer.getWorkbook().createSheet());
        sheetContainer.setModels(models);
        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> generateSheetName = (final SheetContainer sheetContainer) -> {
        final Workbook workbook = workbookContainer.getWorkbook();
        final Sheet sheet = sheetContainer.getSheet();
        final String sheetName;

        final Class<?> clazz = sheetContainer.getModels().get(0).getClass();

        if (clazz.isAnnotationPresent(ExcelSheet.class)) {
            sheetName = clazz.getAnnotation(ExcelSheet.class).name();
        } else {
            sheetName = parseCamelCase(clazz.getSimpleName());
        }

        workbook.setSheetName(workbook.getSheetIndex(sheet), sheetName);

        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> addColumns = (final SheetContainer sheetContainer) -> {
        final Sheet sheet = sheetContainer.getSheet();
        final List<?> models = sheetContainer.getModels();
        final Row row = sheet.createRow(0);

        try {
            final Class<?> clazz = models.get(0).getClass();
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
        final List<?> models = sheetContainer.getModels();

        try {
            final Class<?> clazz = models.get(0).getClass();
            final Field[] fields = FieldUtils.getAllFields(clazz);
            final List<Field> annotatedFields = Arrays.stream(fields)
                    .filter(field -> {
                        field.setAccessible(true);
                        return field.isAnnotationPresent(ExcelCell.class);
                    }).toList();
            final Map<Field, BiConsumer<Cell, Object>> fieldWriter = new HashMap<>();
            annotatedFields.forEach(field -> {
                final BiConsumer<Cell, Object> cellWriter = workbookContainer.getWriterFactory().getFieldWriter(field);
                fieldWriter.put(field, cellWriter);
            });

            IntStream.range(0, models.size()).forEach(rowNum -> {
                final Row row = sheet.createRow(rowNum + 1);
                final Object data = models.get(rowNum);

                final int maxPosition = determineMaxPosition(annotatedFields);
                annotatedFields.forEach(field -> {
                    final int position = determinePosition(field, maxPosition);

                    try {
                        final Cell cell = row.createCell(position);
                        fieldWriter.get(field).accept(cell, data);
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
                .filter(f -> !f.getDeclaringClass().equals(AbstractAuditable.class))
                .map(f -> f.getAnnotation(ExcelCell.class).position())
                .mapToInt(v -> v)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("Could not determine max position"));
        return maxPosition + 1;
    }

    private static int determinePosition(final Field field, final int maxPosition) {
        final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
        return excelCell.position() + (field.getDeclaringClass().equals(AbstractAuditable.class) ? maxPosition : 0);
    }

    private static String parseCamelCase(final String camelCaseString) {
        if (camelCaseString == null) {
            return "";
        } else {
            return capitalize(String.join(" ", splitByCharacterTypeCamelCase(camelCaseString)));
        }
    }
}
