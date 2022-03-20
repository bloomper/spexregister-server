package nu.fgv.register.server.util.impex.util;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Sheet;

@Getter
@Setter
public abstract class AbstractSheetContainer {

    private String overrideSheetName;
    private Sheet sheet;
    private String heading;
}
