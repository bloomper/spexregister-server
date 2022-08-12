package nu.fgv.register.server.util.impex.exporting;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.impex.util.AbstractWorkbookContainer;

@Getter
@Setter
class WorkbookContainer extends AbstractWorkbookContainer {

    private CellWriterFactory writerFactory;

    WorkbookContainer() {
        this.writerFactory = new CellWriterFactory(this);
    }
}
