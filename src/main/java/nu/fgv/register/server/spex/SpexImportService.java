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

package nu.fgv.register.server.spex;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.category.SpexCategoryService;
import nu.fgv.register.server.impex.importing.AbstractImportService;
import nu.fgv.register.server.impex.importing.ImportEngine;
import nu.fgv.register.server.impex.importing.ImportSpec;
import nu.fgv.register.server.impex.model.ImportResultDto;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.util.FileUtil.downloadImage;
import static nu.fgv.register.server.util.FileUtil.isLocalUrl;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SpexImportService extends AbstractImportService {

    private final SpexService service;
    private final SpexCategoryService categoryService;
    private final String baseUrl;

    public SpexImportService(final List<ImportEngine> engines,
                             final SpexService service,
                             final SpexCategoryService categoryService,
                             final MessageSource messageSource,
                             @Value("${spexregister.base-url}") final String baseUrl) {
        super(engines, messageSource);
        this.service = service;
        this.categoryService = categoryService;
        this.baseUrl = baseUrl;
    }

    @Override
    protected List<ImportSpec> getImportSpecs() {
        return List.of(
                ImportSpec.builder()
                        .clazz(SpexImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> service.exists((Long) v),
                                "categoryId", v -> service.exists((Long) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(SpexRevivalImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> categoryService.exists((Long) v)
                        ))
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data, final Locale locale) {
        final ImportSummary summary = new ImportSummary(messageSource, locale);

        handleImport(
                (List<SpexImpexDto>) data.get(SpexImpexDto.class),
                summary,
                "spex.impex.entityName",
                dto -> {
                    final SpexDto spex = service.create(SPEX_MAPPER.toCreateDto(dto));

                    service.addCategory(spex.getId(), dto.getCategoryId());
                    processPoster(spex.getId(), dto.getPosterUrl());
                },
                dto -> {
                    final SpexDto spex = service.partialUpdate(SPEX_MAPPER.toUpdateDto(dto));

                    if (!service.findCategoryBySpex(spex.getId()).getId().equals(dto.getCategoryId())) {
                        service.addCategory(spex.getId(), dto.getCategoryId());
                    }
                    processPoster(spex.getId(), dto.getPosterUrl());
                },
                dto -> service.deleteById(dto.getId())
        );

        handleImport(
                (List<SpexRevivalImpexDto>) data.get(SpexRevivalImpexDto.class),
                summary,
                "spex.impex.revival.entityName",
                dto -> service.addRevival(dto.getParentId(), dto.getYear()),
                dto -> service.addRevival(dto.getParentId(), dto.getYear()),
                dto -> service.deleteRevival(dto.getParentId(), dto.getId())
        );

        return summary.toResult();
    }

    private void processPoster(final Long id, @Nullable final String url) {
        if (hasText(url) && !isLocalUrl(url, baseUrl)) {
            service.savePoster(id, downloadImage(url), null);
        } else if (!hasText(url)) {
            service.deletePoster(id);
        }
    }
}
