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

package nu.fgv.register.server.tag;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ImportEngine;
import nu.fgv.register.server.util.impex.importing.ImportSpec;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class TagImportService extends AbstractImportService {

    private final TagService service;

    public TagImportService(final List<ImportEngine> engines,
                            final TagService service,
                            final MessageSource messageSource) {
        super(engines, messageSource);
        this.service = service;
    }

    @Override
    protected List<ImportSpec> getImportSpecs() {
        return List.of(
                ImportSpec.builder()
                        .clazz(TagImpexDto.class)
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
                (List<TagImpexDto>) data.get(TagImpexDto.class),
                summary,
                "tag.impex.entityName",
                dto -> service.create(TAG_MAPPER.toCreateDto(dto)),
                dto -> service.partialUpdate(TAG_MAPPER.toUpdateDto(dto)),
                dto -> service.deleteById(dto.getId())
        );

        return summary.toResult();
    }
}
