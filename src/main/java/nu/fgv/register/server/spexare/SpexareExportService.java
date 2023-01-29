package nu.fgv.register.server.spexare;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExcelWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpexareExportService extends AbstractExportService {

    private final SpexareService service;
    private final MessageSource messageSource;
    private final ExcelWriter writer = new ExcelWriter();

    @Override
    protected byte[] doExport(final Workbook workbook, final List<Long> ids, final Locale locale) throws IOException {
        return null;
    }

}
