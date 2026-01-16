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
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ImportEngine;
import nu.fgv.register.server.util.impex.importing.ImportSpec;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class NewsImportService extends AbstractImportService {

    private final NewsService service;

    public NewsImportService(final List<ImportEngine> engines, final NewsService service) {
        super(engines);
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

    @Override
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data) {
        final List<NewsImpexDto> dtos = (List<NewsImpexDto>) data.get(NewsImpexDto.class);
        // TODO
        return ImportResultDto.builder().success(true).build();
    }
}
