package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.Constants;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpexExportService {

    private final SpexRepository repository;

    public Pair<String, byte[]> export(final List<Long> ids, final String type) throws IOException {
        final Workbook workbook;
        final String extension;
        switch (type) {
            case Constants.MediaTypes.APPLICATION_XLSX_VALUE -> { workbook = new XSSFWorkbook(); extension = ".xlsx"; }
            case Constants.MediaTypes.APPLICATION_XLS_VALUE -> { workbook = new HSSFWorkbook(); extension = ".xls"; }
            default -> throw new IllegalArgumentException("Unrecognized type");
        }
        return Pair.of(extension, export(workbook, ids));
    }

    private byte[] export(final Workbook workbook, final List<Long> ids) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
        return outputStream.toByteArray();
    }
}
