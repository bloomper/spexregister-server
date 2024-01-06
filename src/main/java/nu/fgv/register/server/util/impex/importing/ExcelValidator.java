package nu.fgv.register.server.util.impex.importing;


import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelImportCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;

import jakarta.validation.ConstraintViolation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePosition;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePositionBeforeAuditableFields;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.isMarkedForCreation;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.isMarkedForUpdate;
import static org.springframework.util.StringUtils.hasText;

public class ExcelValidator {

    final WorkbookContainer workbookContainer = new WorkbookContainer();

    public ImportResultDto validateSheet(
            final MessageSource messageSource,
            final Locale locale,
            final Workbook workbook,
            final Class<?> clazz,
            final Class<?> createClazz,
            final Class<?> updateClazz,
            final Function<Long, Boolean> existenceChecker) {
        return validateSheet(messageSource, locale, workbook, clazz, createClazz, updateClazz, existenceChecker, null);
    }

    public ImportResultDto validateSheet(
            final MessageSource messageSource,
            final Locale locale,
            final Workbook workbook,
            final Class<?> clazz,
            final Function<Long, Boolean> existenceChecker) {
        return validateSheet(messageSource, locale, workbook, clazz, null, null, existenceChecker, null);
    }

    public ImportResultDto validateSheet(
            final MessageSource messageSource,
            final Locale locale,
            final Workbook workbook,
            final Class<?> clazz,
            final Class<?> createClazz,
            final Class<?> updateClazz,
            final Function<Long, Boolean> existenceChecker,
            final String overrideSheetName) {
        workbookContainer.setMessageSource(messageSource);
        workbookContainer.setLocale(locale);
        workbookContainer.setWorkbook(workbook);
        workbookContainer.setCreateClazz(createClazz);
        workbookContainer.setUpdateClazz(updateClazz);
        workbookContainer.setExistenceChecker(existenceChecker);

        initialize
                .andThen(doesSheetExist)
                .andThen(doAllColumnsExist)
                .andThen(doAllExistingEntriesReallyExist)
                .andThen(areUpdatedEntriesValid)
                .apply(clazz, overrideSheetName);

        return ImportResultDto.builder().success(workbookContainer.getMessages().isEmpty()).messages(workbookContainer.getMessages()).build();
    }

    private final BiFunction<Class<?>, String, SheetContainer> initialize = (final Class<?> clazz, final String overrideSheetName) -> {
        final SheetContainer sheetContainer = new SheetContainer();

        sheetContainer.setOverrideSheetName(overrideSheetName);
        sheetContainer.setClazz(clazz);

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> doesSheetExist = (final SheetContainer sheetContainer) -> {
        final Class<?> clazz = sheetContainer.getClazz();
        final String sheetName;

        if (hasText(sheetContainer.getOverrideSheetName())) {
            sheetName = sheetContainer.getOverrideSheetName();
        } else if (clazz.isAnnotationPresent(ExcelSheet.class)) {
            final String name = clazz.getAnnotation(ExcelSheet.class).name();

            sheetName = workbookContainer.getMessageSource().getMessage(name, null, name, workbookContainer.getLocale());
        } else {
            sheetName = parseCamelCase(clazz.getSimpleName());
        }

        final int sheetIndex = workbookContainer.getWorkbook().getSheetIndex(sheetName);
        sheetContainer.setSheetIndex(sheetIndex);

        if (sheetIndex == -1) {
            workbookContainer.getMessages().add(workbookContainer.getMessageSource().getMessage("import.validation.missingSheet", new Object[]{sheetName}, workbookContainer.getLocale()));
        }

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> doAllColumnsExist = (final SheetContainer sheetContainer) -> {
        if (sheetContainer.getSheetIndex() != -1) {
            final Sheet sheet = workbookContainer.getWorkbook().getSheetAt(sheetContainer.getSheetIndex());
            final Field[] fields = FieldUtils.getAllFields(sheetContainer.getClazz());
            final List<Field> annotatedFields = Arrays.stream(fields)
                    .filter(field -> {
                        field.setAccessible(true); // NOSONAR
                        return field.isAnnotationPresent(ExcelCell.class);
                    }).toList();
            final int maxPosition = determinePositionBeforeAuditableFields(annotatedFields);

            annotatedFields.forEach(field -> {
                final ExcelCell excelCell = field.getAnnotation(ExcelCell.class);
                final int position = determinePosition(field, maxPosition);
                String header = excelCell.header();

                if (!hasText(header)) {
                    header = parseCamelCase(field.getName());
                } else {
                    header = workbookContainer.getMessageSource().getMessage(header, null, header, workbookContainer.getLocale());
                }

                if (!sheet.getRow(0).getCell(position).getStringCellValue().equals(header)) {
                    workbookContainer.getMessages().add(workbookContainer.getMessageSource().getMessage("import.validation.columnMismatch", new Object[]{position, header}, workbookContainer.getLocale()));
                }
            });
        }

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> doAllExistingEntriesReallyExist = (final SheetContainer sheetContainer) -> {
        if (sheetContainer.getSheetIndex() != -1) {
            final Sheet sheet = workbookContainer.getWorkbook().getSheetAt(sheetContainer.getSheetIndex());
            final Field[] fields = FieldUtils.getAllFields(workbookContainer.getUpdateClazz());
            final Field primaryKeyField = Arrays.stream(fields)
                    .filter(field -> {
                        field.setAccessible(true); // NOSONAR
                        return field.isAnnotationPresent(ExcelImportCell.class);
                    })
                    .filter(field -> field.getAnnotation(ExcelImportCell.class).primaryKey())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find any field configured to be primary key"));
            sheetContainer.setPrimaryKeyPosition(primaryKeyField.getAnnotation(ExcelImportCell.class).position());

            sheet.rowIterator().forEachRemaining(row -> {
                if (row.getRowNum() != 0) {
                    final Cell primaryKeyCell = row.getCell(sheetContainer.getPrimaryKeyPosition());

                    if (!isMarkedForCreation(primaryKeyCell)) {
                        final Long primaryKey;

                        // TODO: Strip "d" suffix
                        switch (primaryKeyCell.getCellType()) {
                            case STRING ->
                                    primaryKey = hasText(primaryKeyCell.getStringCellValue()) ? Long.parseLong(primaryKeyCell.getStringCellValue()) : null;
                            case NUMERIC ->
                                    primaryKey = (long) primaryKeyCell.getNumericCellValue();
                            default -> {
                                workbookContainer.getMessages().add(workbookContainer.getMessageSource().getMessage("import.validation.cellTypeMismatch", new Object[]{sheetContainer.getPrimaryKeyPosition(), row.getRowNum()}, workbookContainer.getLocale()));
                                primaryKey = null;
                            }
                        }

                        if (primaryKey != null && primaryKey < 0 && Boolean.TRUE.equals(!workbookContainer.getExistenceChecker().apply(primaryKey))) {
                            workbookContainer.getMessages().add(workbookContainer.getMessageSource().getMessage("import.validation.entryDoesNotExist", new Object[]{row.getRowNum()}, workbookContainer.getLocale()));
                        }
                    }
                }
            });
        }

        return sheetContainer;
    };

    private final UnaryOperator<SheetContainer> areUpdatedEntriesValid = (final SheetContainer sheetContainer) -> {
        if (sheetContainer.getSheetIndex() != -1) {
            final Sheet sheet = workbookContainer.getWorkbook().getSheetAt(sheetContainer.getSheetIndex());
            final Field[] fields = FieldUtils.getAllFields(workbookContainer.getUpdateClazz());
            final List<Field> annotatedFields = Arrays.stream(fields)
                    .filter(field -> {
                        field.setAccessible(true); // NOSONAR
                        return field.isAnnotationPresent(ExcelImportCell.class);
                    }).toList();

            sheet.rowIterator().forEachRemaining(row -> {
                if (row.getRowNum() != 0) {
                    final Cell primaryKeyCell = row.getCell(sheetContainer.getPrimaryKeyPosition());

                    if (!isMarkedForUpdate(primaryKeyCell)) {
                        Object dto;
                        try {
                            dto = workbookContainer.getUpdateClazz().getDeclaredConstructor().newInstance();
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                 NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                        annotatedFields.forEach(field -> {
                            final ExcelImportCell excelCell = field.getAnnotation(ExcelImportCell.class);
                            final Cell cell = row.getCell(excelCell.position());

                            field.setAccessible(true); // NOSONAR
                            if (field.getType() == String.class) {
                                try {
                                    field.set(dto, cell.getStringCellValue()); // NOSONAR
                                } catch (IllegalAccessException e) {
                                    // TODO: Fix logging
                                    e.printStackTrace();
                                }
                            }
                        });
                        final Set<ConstraintViolation<Object>> violations = workbookContainer.getValidator().validate(dto);
                        for (ConstraintViolation<Object> violation : violations) {
                            workbookContainer.getMessages().add(violation.getMessage());
                        }
                    }
                }
            });
        }

        return sheetContainer;
    };

    // Deleted entries, ends with D
    // Updated entries, no prefix or suffix
    // New entries, ends with N?
    // OR is better to have an action column? C, U, D? If so, add to export? <---------------------
    // Only care if the action column is filled in? Makes things faster...
    // TODO: Validate existing entries, i.e. are all fields valid
    // TODO: Validate new entries, i.e. are all fields valid
}
