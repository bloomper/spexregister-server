package nu.fgv.register.server.util.impex.exporting;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.impex.util.AbstractSheetContainer;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

@Getter
@Setter
class SheetContainer extends AbstractSheetContainer {

    private List<?> data;
}
