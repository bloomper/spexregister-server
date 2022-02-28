package nu.fgv.register.server.util.export;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

@Getter
@Setter
class SheetContainer {

    private Sheet sheet;
    private String heading;
    private List<?> models;
}
