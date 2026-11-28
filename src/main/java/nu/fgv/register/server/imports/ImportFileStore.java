/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.imports;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.error.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Files uploaded for import records (the spexare's images), kept by their SHA-256 under the impex storage path, for
 * seven days after a record last used them.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@Slf4j
public class ImportFileStore {

    static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Duration KEEP = Duration.ofDays(7);

    private final Path files;
    private final Clock clock = Clock.systemUTC();

    public ImportFileStore(@Value("${spexregister.impex.storage-path}") final Path storage) {
        this.files = storage.resolve("import_files").toAbsolutePath().normalize();
    }

    public static String normalize(final String sha256) {
        final String lower = sha256.strip().toLowerCase(Locale.ROOT);

        if (!SHA256.matcher(lower).matches()) {
            throw new BadRequestException("import.file.hash.invalid");
        }
        return lower;
    }

    public boolean has(final String sha256) {
        return Files.isRegularFile(path(sha256));
    }

    /**
     * Stores the content under its hash; refused when the content has another hash or is too large. Returns false when
     * it was there.
     */
    public boolean put(final String sha256, final InputStream content) {
        final String hash = normalize(sha256);
        final Path target = path(hash);

        if (Files.isRegularFile(target)) {
            touch(target);
            return false;
        }
        try {
            Files.createDirectories(files);

            final Path temporary = Files.createTempFile(files, "upload-", ".part");

            try {
                final MessageDigest digest = MessageDigest.getInstance("SHA-256");

                try (DigestInputStream in = new DigestInputStream(content, digest)) {
                    if (Files.copy(new LimitedInputStream(in, MAX_SIZE), temporary, StandardCopyOption.REPLACE_EXISTING) > MAX_SIZE) {
                        throw new BadRequestException("import.file.tooLarge");
                    }
                }
                if (!HexFormat.of().formatHex(digest.digest()).equals(hash)) {
                    throw new BadRequestException("import.file.hash.mismatch");
                }
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The content of a hash, for a record that uses it; a bad request when it has not been uploaded. */
    public byte[] require(final String sha256) {
        final Path path = path(normalize(sha256));

        if (!Files.isRegularFile(path)) {
            throw new BadRequestException("import.file.missing");
        }
        touch(path);
        try {
            return Files.readAllBytes(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path path(final String sha256) {
        return files.resolve(normalize(sha256));
    }

    private void touch(final Path path) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(clock.instant()));
        } catch (final IOException e) {
            log.debug("Could not touch {}: {}", path, e.getMessage());
        }
    }

    /** Deletes files no record has used for seven days. */
    @Scheduled(cron = "0 30 3 * * *")
    public void cleanUp() {
        if (!Files.isDirectory(files)) {
            return;
        }

        final long before = clock.instant().minus(KEEP).toEpochMilli();

        try (Stream<Path> paths = Files.list(files)) {
            paths.filter(path -> {
                try {
                    return Files.getLastModifiedTime(path).toMillis() < before;
                } catch (final IOException e) {
                    return false;
                }
            }).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (final IOException e) {
                    log.warn("Could not delete {}: {}", path, e.getMessage());
                }
            });
        } catch (final IOException e) {
            log.warn("Could not clean up {}: {}", files, e.getMessage());
        }
    }

    /** Reads at most one byte more than the limit, so that a larger upload is noticed without reading all of it. */
    private static final class LimitedInputStream extends java.io.FilterInputStream {

        private long remaining;

        LimitedInputStream(final InputStream in, final long limit) {
            super(in);
            this.remaining = limit + 1;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }

            final int b = super.read();

            if (b >= 0) {
                remaining--;
            }
            return b;
        }

        @Override
        public int read(final byte[] buffer, final int offset, final int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }

            final int n = super.read(buffer, offset, (int) Math.min(length, remaining));

            if (n > 0) {
                remaining -= n;
            }
            return n;
        }
    }
}
