package nu.fgv.register.server.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.SpexCategoryDto;
import nu.fgv.register.server.spex.SpexCreateDto;
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ExcelValidator;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskImportService extends AbstractImportService {

    private final TaskService service;
    private final TaskCategoryService categoryService;
    private final MessageSource messageSource;
    private final ExcelValidator validator = new ExcelValidator();

    @Override
    protected ImportResultDto doImport(final Workbook workbook, final Locale locale) {
        return null;
    }

    @Override
    protected ImportResultDto doValidate(final Workbook workbook, final Locale locale) {
        final ImportResultDto validationResult = validator.validateSheet(messageSource, locale, workbook, TaskDto.class, SpexCreateDto.class, TaskUpdateDto.class, (id) -> service.findById(id).isPresent());
        final ImportResultDto categoryValidationResult = validator.validateSheet(messageSource, locale, workbook, SpexCategoryDto.class, (id) -> categoryService.findById(id).isPresent());
        final List<String> messages = Stream.concat(
                        validationResult.getMessages().stream(),
                        categoryValidationResult.getMessages().stream())
                .toList();

        return ImportResultDto.builder().success(messages.isEmpty()).messages(messages).build();
    }

}
