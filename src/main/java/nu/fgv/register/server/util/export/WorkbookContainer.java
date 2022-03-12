package nu.fgv.register.server.util.export;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;

import java.util.Locale;

@Getter
@Setter
class WorkbookContainer {

    private MessageSource messageSource;
    private Locale locale;
    private Workbook workbook;
    private CellWriterFactory writerFactory;

    public WorkbookContainer() {
        this.writerFactory = new CellWriterFactory(this);
    }
}
