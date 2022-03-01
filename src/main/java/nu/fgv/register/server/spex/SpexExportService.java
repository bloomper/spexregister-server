package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.export.AbstractExportService;
import nu.fgv.register.server.util.export.ExcelWriter;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpexExportService extends AbstractExportService {

    private final SpexService service;
    private final SpexCategoryService categoryService;
    private final ExcelWriter writer = new ExcelWriter();

    @Override
    protected byte[] export(final Workbook workbook, final List<Long> ids) throws IOException {
        var dtos = retrieveDtos(ids);
        var revivalDtos = retrieveRevivalDtos(dtos.stream().map(SpexDto::getId).collect(Collectors.toList()));
        var categoryDtos = retrieveCategoryDtos();

        writer.createSheet(workbook, dtos);
        writer.createSheet(workbook, revivalDtos, "Revivals");
        writer.createSheet(workbook, categoryDtos)
                .ifPresent(sheet -> {
                    if (sheet instanceof XSSFSheet) {
                        final byte[] red = DefaultIndexedColorMap.getDefaultRGB(IndexedColors.RED.getIndex());
                        ((XSSFSheet) sheet).setTabColor(new XSSFColor(red));
                    }
                });
        return convertWorkbookToByteArray(workbook);
    }

    private List<SpexDto> retrieveDtos(final List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return service.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else {
            return service.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

    private List<SpexDto> retrieveRevivalDtos(final List<Long> parentIds) {
        return service.findRevivalsByParentIds(parentIds, Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    private List<SpexCategoryDto> retrieveCategoryDtos() {
        return categoryService.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
