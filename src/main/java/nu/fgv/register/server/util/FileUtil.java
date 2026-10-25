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
import nu.fgv.register.server.config.SpexregisterConfig;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.InternalErrorException;
import org.jspecify.annotations.Nullable;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Images: detecting and checking their type, and downloading them for imports. SVG with scripts or other active content
 * is refused, and downloads are limited to public http and https addresses.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public class FileUtil {

    public static final String IMAGE_SVG_VALUE = "image/svg+xml";
    /**
     * For a stored image served as is: an SVG opened on its own runs no script and loads nothing.
     */
    public static final String IMAGE_CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/gif", IMAGE_SVG_VALUE);

    private static final int SVG_SNIFF_BYTES = 4096;
    private static final Pattern SVG_ROOT = Pattern.compile(
            "^\\uFEFF?\\s*(<\\?xml[^>]*\\?>\\s*)?((<!--.*?-->|<!DOCTYPE[^>]*>)\\s*)*<svg[\\s>/]",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SVG_ACTIVE_CONTENT = Pattern.compile(
            "<\\s*(script|foreignObject|iframe|embed|object)\\b|\\son\\w+\\s*=|javascript\\s*:|<!ENTITY",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024;
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private FileUtil() {
    }

    /**
     * @param file the bytes
     * @return the media type detected from the content
     */
    public static String detectMimeType(final byte[] file) {
        if (isSvg(file)) {
            return IMAGE_SVG_VALUE;
        }

        try {
            return Optional.ofNullable(Magic.getMagicMatch(file, false))
                    .map(MagicMatch::getMimeType)
                    .orElse("");
        } catch (final MagicException | MagicParseException | MagicMatchNotFoundException _) {
            return "";
        }
    }

    /**
     * @param file the bytes
     * @return the media type, PNG, JPEG, GIF or SVG
     * @throws nu.fgv.register.server.util.error.BadRequestException for any other type, or SVG with active content
     */
    public static String requireSupportedImageMimeType(final byte[] file) {
        final String mimeType = detectMimeType(file);

        if (!SUPPORTED_IMAGE_MIME_TYPES.contains(mimeType)) {
            throw new BadRequestException("Unsupported image type, expected one of %s".formatted(SUPPORTED_IMAGE_MIME_TYPES));
        }

        if (IMAGE_SVG_VALUE.equals(mimeType) && SVG_ACTIVE_CONTENT.matcher(new String(file, StandardCharsets.UTF_8)).find()) {
            throw new BadRequestException("SVG images with scripts, event handlers or embedded content are not supported");
        }

        return mimeType;
    }

    private static boolean isSvg(final byte[] file) {
        return SVG_ROOT.matcher(new String(file, 0, Math.min(file.length, SVG_SNIFF_BYTES), StandardCharsets.UTF_8)).find();
    }

    /**
     * @param url a public http or https address
     * @return the image's bytes, at most 15 MB
     * @throws nu.fgv.register.server.util.error.BadRequestException when the address is not public http or https, the
     *     server does not answer 200 or the image is too large
     */
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

    /**
     * @param url     an address, or null
     * @param baseUrl the application's base address
     * @return whether the address is relative or points at the application itself, such as an image exported from it
     */
    public static boolean isLocalUrl(final @Nullable String url, final String baseUrl) {
        if (url == null) {
            return false;
        }

        try {
            final URI uri = new URI(url.trim());

            if (uri.getScheme() == null && uri.getRawAuthority() == null) {
                return uri.getRawPath() != null && uri.getRawPath().startsWith("/");
            }

            final URI base = new URI(baseUrl);

            return base.getScheme() != null && base.getScheme().equalsIgnoreCase(uri.getScheme())
                    && base.getHost() != null && base.getHost().equalsIgnoreCase(uri.getHost())
                    && effectivePort(base) == effectivePort(uri);
        } catch (final URISyntaxException _) {
            return false;
        }
    }

    /**
     * @param url an address of the application
     * @return the same path and query on the public base address
     */
    public static String toPublicUrl(final String url) {
        return toPublicUrl(url, ApplicationContextHolder.getBean(SpexregisterConfig.class).getBaseUrl());
    }

    /**
     * @param url     an address of the application
     * @param baseUrl the public base address
     * @return the same path and query on the base address
     */
    public static String toPublicUrl(final String url, final String baseUrl) {
        final URI uri = URI.create(url);

        return UriComponentsBuilder.fromUriString(baseUrl)
                .replacePath(uri.getRawPath())
                .replaceQuery(uri.getRawQuery())
                .fragment(null)
                .build(true)
                .toUriString();
    }

    private static int effectivePort(final URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }

        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
