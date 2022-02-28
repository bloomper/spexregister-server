package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.export.AbstractExportService;
import nu.fgv.register.server.util.export.ExcelWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpexCategoryExportService extends AbstractExportService {

    private final SpexCategoryService service;
    private final ExcelWriter writer = new ExcelWriter();

    protected byte[] export(final Workbook workbook, final List<Long> ids) throws IOException {
        var dtos = retrieveDtos(ids);

        writer.createSheet(workbook, dtos);
        return convertWorkbookToByteArray(workbook);
    }

    private List<SpexCategoryDto> retrieveDtos(final List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return service.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else {
            return service.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

}
