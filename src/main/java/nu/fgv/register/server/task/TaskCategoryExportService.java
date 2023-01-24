package nu.fgv.register.server.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExcelWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskCategoryExportService extends AbstractExportService {

    private final TaskCategoryService service;
    private final MessageSource messageSource;
    private final ExcelWriter writer = new ExcelWriter();

    protected byte[] doExport(final Workbook workbook, final List<Long> ids, final Locale locale) throws IOException {
        var dtos = retrieveDtos(ids);

        writer.createSheet(messageSource, locale, workbook, dtos);
        return convertWorkbookToByteArray(workbook);
    }

    private List<TaskCategoryDto> retrieveDtos(final List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return service.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else {
            return service.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

}
