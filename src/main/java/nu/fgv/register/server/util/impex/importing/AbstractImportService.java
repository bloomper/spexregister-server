package nu.fgv.register.server.util.impex.importing;

import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

public abstract class AbstractImportService {

    public ImportResultDto doImport(final byte[] file, final String type, final Locale locale) throws IOException {
        try (final var workbook = convertByteArrayToWorkbook(file, type)) {
            final var validationResult = doValidate(workbook, locale);

            return validationResult.isSuccess() ? doImport(workbook, locale) : validationResult;
        }
    }

    protected abstract ImportResultDto doImport(final Workbook workbook, final Locale locale);

    protected abstract ImportResultDto doValidate(final Workbook workbook, final Locale locale);

    private Workbook convertByteArrayToWorkbook(final byte[] file, final String type) throws IOException {
        final var inputStream = new ByteArrayInputStream(file);
        switch (type) {
            case Constants.MediaTypes.APPLICATION_XLSX_VALUE -> {
                return new XSSFWorkbook(inputStream);
            }
            case Constants.MediaTypes.APPLICATION_XLS_VALUE -> {
                return new HSSFWorkbook(inputStream);
            }
            default -> throw new IllegalArgumentException("Unrecognized type");
        }
    }

}
