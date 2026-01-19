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

package nu.fgv.register.server.impex.exporting.pdf;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.exporting.ExportEngine;
import nu.fgv.register.server.impex.exporting.ExportHolder;
import nu.fgv.register.server.impex.model.ExportType;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Component
public class PdfExportEngine implements ExportEngine {

    private final List<PdfGenerator> generators;

    @Override
    public byte[] export(final List<ExportHolder<?>> reports, final Locale locale, final String contentType, @Nullable final ExportType type) {
        if (type == null) {
            throw new IllegalArgumentException("Export type must be specified for PDF export");
        }

        return generators.stream()
                .filter(g -> g.supports(type))
                .findFirst()
                .map(g -> g.generate(reports, locale))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported PDF export type: " + type));
    }

    @Override
    public boolean supports(final String contentType) {
        return MediaType.APPLICATION_PDF_VALUE.equals(contentType);
    }

    @Override
    public String getExtension(final String contentType) {
        return ".pdf";
    }
}
