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
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.InternalErrorException;

import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class FileUtil {

    private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/gif");

    private static final int MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024;
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

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

    public static String requireSupportedImageMimeType(final byte[] file) {
        final String mimeType = detectMimeType(file);

        if (!SUPPORTED_IMAGE_MIME_TYPES.contains(mimeType)) {
            throw new BadRequestException("Unsupported image type, expected one of %s".formatted(SUPPORTED_IMAGE_MIME_TYPES));
        }

        return mimeType;
    }

    public static byte[] downloadImage(final String url) {
        final URI uri = requirePublicHttpUri(url);

        try {
            final HttpResponse<InputStream> response = HTTP_CLIENT.send(
                    HttpRequest.newBuilder(uri).timeout(DOWNLOAD_TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());

            try (final InputStream in = response.body()) {
                if (response.statusCode() != 200) {
                    throw new BadRequestException("Could not download image from %s: HTTP %d".formatted(url, response.statusCode()));
                }

                final byte[] content = in.readNBytes(MAX_DOWNLOAD_BYTES + 1);

                if (content.length > MAX_DOWNLOAD_BYTES) {
                    throw new BadRequestException("Image at %s is larger than %d bytes".formatted(url, MAX_DOWNLOAD_BYTES));
                }

                return content;
            }
        } catch (final BadRequestException e) {
            throw e;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalErrorException("Interrupted while downloading image from: " + url);
        } catch (final Exception e) {
            throw new InternalErrorException("Could not download image from: " + url);
        }
    }

    static URI requirePublicHttpUri(final String url) {
        final URI uri;

        try {
            uri = new URI(url.trim());
        } catch (final URISyntaxException _) {
            throw new BadRequestException("Invalid image URL: " + url);
        }

        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new BadRequestException("Image URL must be http or https: " + url);
        }

        try {
            for (final InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress() || isUniqueLocal(address)) {
                    throw new BadRequestException("Image URL must not point at an internal address: " + url);
                }
            }
        } catch (final UnknownHostException _) {
            throw new BadRequestException("Unknown host in image URL: " + url);
        }

        return uri;
    }

    private static boolean isUniqueLocal(final InetAddress address) {
        return address instanceof Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc;
    }

    public static boolean isLocalUrl(final String url, final String baseUrl) {
        return url != null && url.startsWith(baseUrl);
    }
}
