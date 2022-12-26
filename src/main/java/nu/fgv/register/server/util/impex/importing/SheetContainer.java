package nu.fgv.register.server.util.impex.importing;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.impex.util.AbstractSheetContainer;

@Getter
@Setter
class SheetContainer extends AbstractSheetContainer {

    private int sheetIndex;
    private int primaryKeyPosition;
    private Class<?> clazz;
}
