package nu.fgv.register.server.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExcelWriter;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskExportService extends AbstractExportService {

    private final TaskService service;
    private final TaskCategoryService categoryService;
    private final MessageSource messageSource;
    private final ExcelWriter writer = new ExcelWriter();

    @Override
    protected byte[] doExport(final Workbook workbook, final List<Long> ids, final Locale locale) throws IOException {
        var dtos = retrieveDtos(ids);
        var categoryDtos = retrieveCategoryDtos();

        writer.createSheet(messageSource, locale, workbook, dtos);
        writer.createSheet(messageSource, locale, workbook, categoryDtos)
                .ifPresent(sheet -> {
                    if (sheet instanceof XSSFSheet) {
                        final byte[] red = DefaultIndexedColorMap.getDefaultRGB(IndexedColors.RED.getIndex());
                        ((XSSFSheet) sheet).setTabColor(new XSSFColor(red));
                    }
                    sheet.protectSheet("");
                });

        return convertWorkbookToByteArray(workbook);
    }

    private List<TaskDto> retrieveDtos(final List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return service.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else {
            return service.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

    private List<TaskCategoryDto> retrieveCategoryDtos() {
        return categoryService.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
