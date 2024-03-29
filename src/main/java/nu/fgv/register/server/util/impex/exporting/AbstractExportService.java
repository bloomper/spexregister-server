package nu.fgv.register.server.util.impex.exporting;

import nu.fgv.register.server.util.Constants;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public abstract class AbstractExportService {

    public Pair<String, byte[]> doExport(final List<Long> ids, final String type, final Locale locale) throws IOException {
        final Workbook workbook;
        final String extension;
        switch (type) {
            case Constants.MediaTypes.APPLICATION_XLSX_VALUE -> {
                workbook = new XSSFWorkbook();
                extension = ".xlsx";
            }
            case Constants.MediaTypes.APPLICATION_XLS_VALUE -> {
                workbook = new HSSFWorkbook();
                extension = ".xls";
            }
            default -> throw new IllegalArgumentException("Unrecognized type");
        }
        return Pair.of(extension, doExport(workbook, ids, locale));
    }

    protected abstract byte[] doExport(final Workbook workbook, final List<Long> ids, final Locale locale) throws IOException;

    protected byte[] convertWorkbookToByteArray(final Workbook workbook) throws IOException {
        final var outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
        return outputStream.toByteArray();
    }
}
