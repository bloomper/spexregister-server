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

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.Caller;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Runs import records through their handlers, one transaction per record, and keeps track of what each source key made
 * (the import API shared by the FGV applications, Spexipedia's {@code docs/content-import.md}, section 4):
 * <ul>
 * <li>a new source key creates a record;</li>
 * <li>a known one updates the record it made, unless the content is the same ({@code UNCHANGED}) or someone has
 * changed or removed the record since ({@code KEPT});</li>
 * <li>a failure is reported for its record and does not stop the others;</li>
 * <li>a dry run does the same and rolls every record back.</li>
 * </ul>
 * Records are written through the register's services as {@value #AUDITOR}, which the audit trail records as who made
 * the changes.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@Slf4j
public class ImportEngine {

    public static final int MAX_RECORDS = 500;
    public static final String AUDITOR = "spexregister-migration";

    private final ImportKeyRepository keys;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final JsonMapper json;
    private final Clock clock = Clock.systemUTC();

    public ImportEngine(final ImportKeyRepository keys, final PlatformTransactionManager transactionManager,
                        final EntityManager entityManager, final JsonMapper json) {
        this.keys = keys;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.entityManager = entityManager;
        this.json = json;
    }

    /**
     * What one kind of record needs from its handler.
     *
     * @param <T> the import record
     */
    public interface Handler<T> {

        /** The kind of record, e.g. {@code SPEXARE}, as kept in {@code import_key}. */
        String kind();

        String sourceKey(T record);

        /** Creates the record (existing is null) or updates the one an earlier import made, and returns its id. */
        UUID apply(T record, @Nullable UUID existing, Batch batch);

        /** A fingerprint of what the import writes for the record, or null when it is gone. */
        @Nullable Long version(UUID id);

        /** Removes a record an import made and nobody changed; returns whether it was removed. */
        boolean forget(UUID id);
    }

    /**
     * One call of the import API: whether it is a dry run, and the warnings of the record being imported.
     */
    public static final class Batch {

        private final boolean dryRun;
        private final List<ImportText> warnings = new ArrayList<>();
        private final Set<String> planned = new HashSet<>();

        public Batch(final boolean dryRun) {
            this.dryRun = dryRun;
        }

        public boolean dryRun() {
            return dryRun;
        }

        /** A warning about the record being imported: a message code and its arguments. */
        public void warn(final String code, final Object... args) {
            warnings.add(ImportText.of(code, args));
        }

        /**
         * Whether an earlier record of this call has the source key and went well. In a dry run nothing it made is
         * kept, so records that refer to it check what they can without it.
         */
        public boolean planned(final @Nullable String sourceKey) {
            return sourceKey != null && planned.contains(sourceKey.strip());
        }

        List<ImportText> drainWarnings() {
            final List<ImportText> drained = List.copyOf(warnings);

            warnings.clear();
            return drained;
        }
    }

    public enum Outcome {
        CREATED,
        UPDATED,
        UNCHANGED,
        /** Someone changed or removed the record since it was imported, so the import left it alone. */
        KEPT,
        FAILED
    }

    /** One record's outcome; its message and warnings are put in a language when read. */
    public record ItemResult(String kind, String sourceKey, @Nullable UUID id, Outcome outcome, @Nullable ImportText message,
                             List<ImportText> warnings) {
    }

    /** Imports the records of one kind, in order. */
    public <T> List<ItemResult> run(final Handler<T> handler, final @Nullable List<T> records, final Batch batch, final String importedBy) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        final List<ItemResult> results = new ArrayList<>();

        for (final T record : records) {
            results.add(asAuditor(() -> one(handler, record, batch, importedBy)));
        }
        return results;
    }

    private <T> ItemResult one(final Handler<T> handler, final T record, final Batch batch, final String importedBy) {
        final String sourceKey = Objects.requireNonNullElse(handler.sourceKey(record), "").strip();

        if (sourceKey.isEmpty() || sourceKey.length() > 1000) {
            return new ItemResult(handler.kind(), sourceKey, null, Outcome.FAILED, ImportText.of("import.failed.sourceKey"), List.of());
        }
        try {
            final ItemResult result = transactionTemplate.execute(status -> {
                final ItemResult item = inTransaction(handler, record, sourceKey, batch, importedBy);

                if (batch.dryRun()) {
                    status.setRollbackOnly();
                }
                return item;
            });

            if (Objects.requireNonNull(result).outcome() != Outcome.FAILED) {
                batch.planned.add(sourceKey);
            }
            return result;
        } catch (final RuntimeException e) {
            if (!(e instanceof ResponseStatusException)) {
                log.warn("Import of {} {} failed", handler.kind(), sourceKey, e);
            }
            return new ItemResult(handler.kind(), sourceKey, null, Outcome.FAILED, message(e), batch.drainWarnings());
        }
    }

    private <T> ItemResult inTransaction(final Handler<T> handler, final T record, final String sourceKey, final Batch batch,
                                         final String importedBy) {
        final String sourceHash = sha256(sourceKey);
        final String contentHash = sha256(json.writeValueAsString(record));
        final ImportKey key = keys.findBySourceHash(sourceHash).orElse(null);

        if (key != null && !key.getRecordKind().equals(handler.kind())) {
            return new ItemResult(handler.kind(), sourceKey, null, Outcome.FAILED, ImportText.of("import.failed.sourceKeyKind", key.getRecordKind()), List.of());
        }

        final Outcome outcome;
        final UUID id;

        if (key == null) {
            id = handler.apply(record, null, batch);
            outcome = Outcome.CREATED;
        } else {
            final Long current = handler.version(key.getRecordId());

            if (current == null || (key.getRecordVersion() != null && !current.equals(key.getRecordVersion()))) {
                return new ItemResult(handler.kind(), sourceKey, key.getRecordId(), Outcome.KEPT, null, batch.drainWarnings());
            }
            if (contentHash.equals(key.getImportedHash())) {
                return new ItemResult(handler.kind(), sourceKey, key.getRecordId(), Outcome.UNCHANGED, null, batch.drainWarnings());
            }
            id = handler.apply(record, key.getRecordId(), batch);
            outcome = Outcome.UPDATED;
        }
        entityManager.flush();

        final ImportKey saved = key == null ? new ImportKey() : key;

        if (key == null) {
            saved.setSourceKey(sourceKey);
            saved.setSourceHash(sourceHash);
            saved.setRecordKind(handler.kind());
            saved.setCreatedAt(clock.instant());
        }
        saved.setRecordId(id);
        saved.setImportedHash(contentHash);
        saved.setRecordVersion(handler.version(id));
        saved.setImportedBy(importedBy);
        saved.setUpdatedAt(clock.instant());
        keys.save(saved);
        return new ItemResult(handler.kind(), sourceKey, id, outcome, null, batch.drainWarnings());
    }

    /** The id of the record a source key made, if any; for references between records. */
    public @Nullable UUID recordOf(final String kind, final @Nullable String sourceKey) {
        if (sourceKey == null || sourceKey.isBlank()) {
            return null;
        }
        return keys.findBySourceHash(sha256(sourceKey.strip()))
                .filter(key -> key.getRecordKind().equals(kind))
                .map(ImportKey::getRecordId)
                .orElse(null);
    }

    /**
     * Removes what was imported under a source prefix and is still unchanged; forgets the keys of what is gone. Newest
     * first, so that records are removed before what they refer to. Returns how many were removed.
     */
    public int forget(final String prefix, final List<Handler<?>> handlers) {
        int removed = 0;

        for (final ImportKey key : keys.findBySourcePrefix(prefix.strip())) {
            final Handler<?> handler = handlers.stream().filter(h -> h.kind().equals(key.getRecordKind())).findFirst().orElse(null);

            if (handler == null) {
                continue;
            }

            final Boolean done = asAuditor(() -> transactionTemplate.execute(_ -> {
                final Long version = handler.version(key.getRecordId());

                if (version == null) {
                    keys.delete(key);
                    return false;
                }
                if (key.getRecordVersion() != null && !version.equals(key.getRecordVersion())) {
                    return false;
                }
                if (handler.forget(key.getRecordId())) {
                    keys.delete(key);
                    return true;
                }
                return false;
            }));

            if (Boolean.TRUE.equals(done)) {
                removed++;
            }
        }
        return removed;
    }

    /** The service account's subject, for {@code imported_by}. */
    public static String caller() {
        return Objects.requireNonNullElse(Caller.current().subject(), AUDITOR);
    }

    private static <R> R asAuditor(final Supplier<R> work) {
        final List<R> result = new ArrayList<>(1);

        SecurityUtil.runAs(AUDITOR, () -> result.add(work.get()));
        return result.getFirst();
    }

    private static ImportText message(final RuntimeException e) {
        if (e instanceof ResponseStatusException status && status.getReason() != null) {
            // A message code of the register's, or a text when the code is unknown
            return ImportText.of(status.getReason());
        }
        return ImportText.of("import.failed.unexpected", e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
    }

    /** A 64-bit fingerprint of text, for {@link Handler#version}. */
    public static long fingerprint(final String text) {
        final byte[] digest = sha256Bytes(text);
        long value = 0;

        for (int i = 0; i < Long.BYTES; i++) {
            value = (value << 8) | (digest[i] & 0xFF);
        }
        return value;
    }

    public static String sha256(final String text) {
        return HexFormat.of().formatHex(sha256Bytes(text));
    }

    private static byte[] sha256Bytes(final String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
