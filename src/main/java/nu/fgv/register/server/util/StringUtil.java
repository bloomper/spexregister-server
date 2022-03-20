package nu.fgv.register.server.util;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

public class StringUtil {

    private StringUtil() {
    }

    public static String parseCamelCase(final String camelCaseString) {
        if (camelCaseString == null) {
            return "";
        } else {
            return capitalize(String.join(" ", splitByCharacterTypeCamelCase(camelCaseString)));
        }
    }
}
