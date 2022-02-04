package nu.fgv.register.server.util;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

import java.util.Optional;

public class FileUtil {

    private FileUtil() {
    }

    public static String detectMimeType(final byte[] file) {
        try {
            return Optional.ofNullable(Magic.getMagicMatch(file, false))
                    .map(MagicMatch::getMimeType)
                    .orElse(null);
        } catch (MagicException | MagicParseException | MagicMatchNotFoundException e) {
            return null;
        }

    }
}
