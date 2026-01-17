/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.util;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
import nu.fgv.register.server.util.error.InternalErrorException;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class FileUtil {

    private FileUtil() {
    }

    public static String detectMimeType(final byte[] file) {
        try {
            return Optional.ofNullable(Magic.getMagicMatch(file, false))
                    .map(MagicMatch::getMimeType)
                    .orElse("");
        } catch (final MagicException | MagicParseException | MagicMatchNotFoundException _) {
            return "";
        }
    }

    public static byte[] downloadImage(final String url) {
        try (final InputStream in = new URI(url).toURL().openStream()) {
            return in.readAllBytes();
        } catch (final Exception e) {
            throw new InternalErrorException("Could not download image from: " + url);
        }
    }

    public static boolean isLocalUrl(final String url, final String baseUrl) {
        return url != null && url.startsWith(baseUrl);
    }
}
