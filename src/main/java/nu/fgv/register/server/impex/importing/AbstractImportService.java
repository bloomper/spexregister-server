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

package nu.fgv.register.server.impex.importing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.error.ImportException;
import nu.fgv.register.server.impex.model.HasImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
public abstract class AbstractImportService {

    protected final List<ImportEngine> engines;
    protected final MessageSource messageSource;

    protected AbstractImportService(final List<ImportEngine> engines,
                                    final MessageSource messageSource) {
        this.engines = engines;
        this.messageSource = messageSource;
    }

    public ImportResultDto doImport(final byte[] file, @Nullable final String type, final Locale locale) {
        final ImportEngine engine = engines.stream()
                .filter(e -> e.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported import type: " + type));

        final List<ImportSpec> specs = getImportSpecs();

        try {
            final ImportEngineResponse response = engine.process(file, specs, locale);

            if (response.validationResult().isSuccess() && response.data() != null) {
                return processImport(response.data(), locale);
            }

            return response.validationResult();
        } catch (final Exception e) {
            log.error("Unexpected error while importing", e);
            throw new ImportException(e.getMessage());
        }
    }

    protected abstract List<ImportSpec> getImportSpecs();

    protected abstract ImportResultDto processImport(final Map<Class<?>, List<?>> data, final Locale locale);

    protected <T extends HasImpexAction> void handleImport(@Nullable final List<T> dtos,
                                                           final ImportSummary summary,
                                                           final String entity,
                                                           final Consumer<T> creator,
                                                           final Consumer<T> updater,
                                                           final Consumer<T> deleter) {
        if (dtos != null) {
            dtos.forEach(dto -> {
                try {
                    switch (dto.getAction()) {
                        case CREATE -> {
                            creator.accept(dto);
                            summary.incrementCreated(entity);
                        }
                        case UPDATE -> {
                            updater.accept(dto);
                            summary.incrementUpdated(entity);
                        }
                        case DELETE -> {
                            deleter.accept(dto);
                            summary.incrementDeleted(entity);
                        }
                    }
                } catch (final Exception e) {
                    log.error("Failed to process {} import for row", entity, e);
                    summary.addError(dto.getRowNumber(), entity, e.getMessage());
                }
            });
        }
    }

    @Getter
    public static class ImportSummary {
        private final Map<String, Integer> createdCounts = new HashMap<>();
        private final Map<String, Integer> updatedCounts = new HashMap<>();
        private final Map<String, Integer> deletedCounts = new HashMap<>();
        private final List<String> errors = new ArrayList<>();
        private final MessageSource messageSource;
        private final Locale locale;

        public ImportSummary(final MessageSource messageSource, final Locale locale) {
            this.messageSource = messageSource;
            this.locale = locale;
        }

        public void incrementCreated(final String entity) {
            createdCounts.merge(entity, 1, Integer::sum);
        }

        public void incrementUpdated(final String entity) {
            updatedCounts.merge(entity, 1, Integer::sum);
        }

        public void incrementDeleted(final String entity) {
            deletedCounts.merge(entity, 1, Integer::sum);
        }

        public void addError(@Nullable final Integer rowNumber, final String entity, final String error) {
            final String translatedEntity = translateEntity(entity, 1);
            errors.add(messageSource.getMessage("common.impex.summary.error", new Object[]{rowNumber != null ? rowNumber : "?", translatedEntity, error}, locale));
        }

        public List<String> getMessages() {
            final List<String> messages = new ArrayList<>();

            appendSummary(messages, messageSource.getMessage("common.impex.summary.created", null, locale), createdCounts);
            appendSummary(messages, messageSource.getMessage("common.impex.summary.updated", null, locale), updatedCounts);
            appendSummary(messages, messageSource.getMessage("common.impex.summary.deleted", null, locale), deletedCounts);

            return messages;
        }

        private void appendSummary(final List<String> messages, final String action, final Map<String, Integer> counts) {
            if (!counts.isEmpty()) {
                final String details = counts.entrySet().stream()
                        .map(e -> String.format("%d %s", e.getValue(), translateEntity(e.getKey(), e.getValue())))
                        .collect(Collectors.joining(", "));
                messages.add(action + ": " + details);
            }
        }

        private String translateEntity(final String entity, final int count) {
            final String suffix = (count == 1) ? "" : ".plural";

            return messageSource.getMessage(entity + suffix, null, entity, locale);
        }

        public ImportResultDto toResult() {
            return toResult(null);
        }

        public ImportResultDto toResult(@Nullable final Map<String, Object> data) {
            return ImportResultDto.builder()
                    .success(errors.isEmpty())
                    .messages(getMessages())
                    .errors(errors)
                    .data(data)
                    .build();
        }
    }
}
