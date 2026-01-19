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

package nu.fgv.register.server.news;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.impex.importing.AbstractImportService;
import nu.fgv.register.server.impex.importing.ImportEngine;
import nu.fgv.register.server.impex.importing.ImportSpec;
import nu.fgv.register.server.impex.model.ImportResultDto;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.news.NewsMapper.NEWS_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class NewsImportService extends AbstractImportService {

    private final NewsService service;

    public NewsImportService(final List<ImportEngine> engines,
                             final NewsService service,
                             final MessageSource messageSource) {
        super(engines, messageSource);
        this.service = service;
    }

    @Override
    protected List<ImportSpec> getImportSpecs() {
        return List.of(
                ImportSpec.builder()
                        .clazz(NewsImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> service.exists((Long) v)
                        ))
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data, final Locale locale) {
        final ImportSummary summary = new ImportSummary(messageSource, locale);

        handleImport(
                (List<NewsImpexDto>) data.get(NewsImpexDto.class),
                summary,
                "news.impex.entityName",
                dto -> service.create(NEWS_MAPPER.toCreateDto(dto)),
                dto -> service.partialUpdate(NEWS_MAPPER.toUpdateDto(dto)),
                dto -> service.deleteById(dto.getId())
        );

        return summary.toResult();
    }
}
