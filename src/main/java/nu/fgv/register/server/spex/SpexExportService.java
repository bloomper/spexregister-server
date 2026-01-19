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
import nu.fgv.register.server.impex.exporting.AbstractExportService;
import nu.fgv.register.server.impex.exporting.ExportEngine;
import nu.fgv.register.server.impex.exporting.ExportHolder;
import nu.fgv.register.server.spex.category.SpexCategoryImpexDto;
import nu.fgv.register.server.spex.category.SpexCategoryService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SpexExportService extends AbstractExportService {

    private final SpexService service;
    private final SpexCategoryService categoryService;

    public SpexExportService(final SpexService service, final SpexCategoryService categoryService, final List<ExportEngine> engines) {
        super(engines);
        this.service = service;
        this.categoryService = categoryService;
    }

    @Override
    protected List<ExportHolder<?>> getReports(final List<Long> ids, final String filter) {
        return List.of(
                ExportHolder.of(
                        toImpexDto(service.streamByIds(ids, filter, Sort.by(Sort.Direction.ASC, "year")), SPEX_MAPPER::toImpexDto),
                        SpexImpexDto.class
                ),
                ExportHolder.of(
                        toImpexDto(service.streamRevivalsByParentIds(ids, filter, Sort.by(Sort.Direction.ASC, "year")), SPEX_MAPPER::toRevivalImpexDto),
                        SpexRevivalImpexDto.class
                ),
                ExportHolder.of(
                        toImpexDto(categoryService.streamByIds(Collections.emptyList(), "", Sort.by(Sort.Direction.ASC, "name")), SPEX_CATEGORY_MAPPER::toImpexDto),
                        SpexCategoryImpexDto.class,
                        true
                )
        );
    }

}
