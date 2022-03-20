package nu.fgv.register.server.util.impex.importing;


import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePosition;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePositionBeforeAuditableFields;
import static org.springframework.util.StringUtils.hasText;

public class ExcelValidator {

    final WorkbookContainer workbookContainer = new WorkbookContainer();

    public ImportResultDto validateSheet(final MessageSource messageSource, final Locale locale, final Workbook workbook, final Class<?> clazz) {
        return validateSheet(messageSource, locale, workbook, clazz, null);
    }

    public ImportResultDto validateSheet(final MessageSource messageSource, final Locale locale, final Workbook workbook, final Class<?> clazz, final String overrideSheetName) {
        workbookContainer.setMessageSource(messageSource);
        workbookContainer.setLocale(locale);
        workbookContainer.setWorkbook(workbook);

        initialize
                .andThen(doesSheetExist)
                .andThen(doAllColumnsExist)
                .apply(clazz, overrideSheetName);

        return ImportResultDto.builder().success(workbookContainer.getMessages().isEmpty()).messages(workbookContainer.getMessages()).build();
    }

    private final BiFunction<Class<?>, String, SheetContainer> initialize = (final Class<?> clazz, final String overrideSheetName) -> {
        final SheetContainer sheetContainer = new SheetContainer();

        sheetContainer.setOverrideSheetName(overrideSheetName);
        sheetContainer.setClazz(clazz);

        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> doesSheetExist = (final SheetContainer sheetContainer) -> {
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
            workbookContainer.getMessages().add(String.format("Missing sheet with name %s", sheetName));
        }

        return sheetContainer;
    };

    private final Function<SheetContainer, SheetContainer> doAllColumnsExist = (final SheetContainer sheetContainer) -> {
        if (sheetContainer.getSheetIndex() != -1) {
            final Sheet sheet = workbookContainer.getWorkbook().getSheetAt(sheetContainer.getSheetIndex());
            final Field[] fields = FieldUtils.getAllFields(sheetContainer.getClazz());
            final List<Field> annotatedFields = Arrays.stream(fields)
                    .filter(field -> {
                        field.setAccessible(true);
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
                    workbookContainer.getMessages().add(String.format("Expected column header %s to be '%s'", position, header));
                }
            });
        }

        return sheetContainer;
    };
}
