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

package nu.fgv.register.server.util.impex.importing;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.error.ImportException;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
public abstract class AbstractImportService {

    protected final List<ImportEngine> engines;

    protected AbstractImportService(final List<ImportEngine> engines) {
        this.engines = engines;
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
                return processImport(response.data());
            }

            return response.validationResult();
        } catch (final Exception e) {
            log.error("Unexpected error while importing", e);
            throw new ImportException(e.getMessage());
        }
    }

    protected abstract List<ImportSpec> getImportSpecs();

    protected abstract ImportResultDto processImport(final Map<Class<?>, List<?>> data);

}
