package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.exporting.ExcelWriter;
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ExcelValidator;
import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static nu.fgv.register.server.util.StringUtil.parseCamelCase;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePosition;
import static nu.fgv.register.server.util.impex.util.ImpexUtil.determinePositionBeforeAuditableFields;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpexCategoryImportService extends AbstractImportService {

    private final SpexCategoryService service;
    private final MessageSource messageSource;
    private final ExcelValidator validator = new ExcelValidator();

    @Override
    protected ImportResultDto doImport(final Workbook workbook, final Locale locale) {
        return null;
    }

    @Override
    protected ImportResultDto doValidate(final Workbook workbook, final Locale locale) {
        return validator.validateSheet(messageSource, locale, workbook, SpexCategoryDto.class);
    }

}
