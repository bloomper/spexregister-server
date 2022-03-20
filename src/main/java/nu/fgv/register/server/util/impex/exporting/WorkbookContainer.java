package nu.fgv.register.server.util.impex.exporting;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.impex.util.AbstractWorkbookContainer;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;

import java.util.Locale;

@Getter
@Setter
class WorkbookContainer extends AbstractWorkbookContainer {

    private CellWriterFactory writerFactory;

    public WorkbookContainer() {
        this.writerFactory = new CellWriterFactory(this);
    }
}
