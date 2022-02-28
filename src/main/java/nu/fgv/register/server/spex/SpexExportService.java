package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.export.AbstractExportService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpexExportService extends AbstractExportService {

    private final SpexRepository repository;

    @Override
    protected byte[] export(final Workbook workbook, final List<Long> ids) throws IOException {
        return convertWorkbookToByteArray(workbook);
    }
}
