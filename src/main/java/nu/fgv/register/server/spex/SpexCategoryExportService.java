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

    private final SpexCategoryRepository repository;
    private final ExcelWriter writer = new ExcelWriter();

    protected byte[] export(final Workbook workbook, final List<Long> ids) throws IOException {
        var models = retrieveModels(ids);

        writer.createSheet(workbook, models);
        return convertWorkbookToByteArray(workbook);
    }

    private List<SpexCategory> retrieveModels(final List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return repository.findAll();
        } else {
            return repository.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

}
