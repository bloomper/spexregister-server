package nu.fgv.register.server.util.impex.util;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;

import java.util.Locale;

@Getter
@Setter
public abstract class AbstractWorkbookContainer {

    protected MessageSource messageSource;
    protected Locale locale;
    protected Workbook workbook;

}
