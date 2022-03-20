package nu.fgv.register.server.util.impex.importing;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.impex.util.AbstractWorkbookContainer;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
class WorkbookContainer extends AbstractWorkbookContainer {
    private final List<String> messages = new ArrayList<>();
}
