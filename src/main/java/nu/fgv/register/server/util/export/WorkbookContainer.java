package nu.fgv.register.server.util.export;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Workbook;

@Getter
@Setter
class WorkbookContainer {

    private Workbook workbook;
    private CellWriterFactory writerFactory;

    public WorkbookContainer() {
        this.writerFactory = new CellWriterFactory(this);
    }
}
