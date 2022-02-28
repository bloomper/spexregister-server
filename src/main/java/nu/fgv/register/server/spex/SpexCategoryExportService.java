package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nu.fgv.register.server.util.export.ExcelWriter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import nu.fgv.register.server.util.Constants;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpexCategoryExportService {

    private final SpexCategoryRepository repository;

    public Pair<String, byte[]> export(final List<Long> ids, final String type) throws IOException {
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
        return Pair.of(extension, export(workbook, ids));
    }

    private byte[] export(final Workbook workbook, final List<Long> ids) throws IOException {
        var models = retrieveModels(ids);
        var sheet = new ExcelWriter().createSheet(workbook, models);

        var outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
        return outputStream.toByteArray();
    }

    private List<SpexCategory> retrieveModels(final List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return repository.findAll();
        } else {
            return repository.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

}
