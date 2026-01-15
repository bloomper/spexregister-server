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

package nu.fgv.register.server.spex.category;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExportEngine;
import nu.fgv.register.server.util.impex.model.ReportHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SpexCategoryExportService extends AbstractExportService {

    private final SpexCategoryService service;

    public SpexCategoryExportService(final SpexCategoryService service, final List<ExportEngine> engines) {
        super(engines);
        this.service = service;
    }

    @Override
    protected List<ReportHolder<?>> getReports(final List<Long> ids) {
        return List.of(
                ReportHolder.of(
                        toImpexDto(service.streamByIds(ids, Sort.by(Sort.Direction.ASC, "name")), SPEX_CATEGORY_MAPPER::toImpexDto),
                        SpexCategoryImpexDto.class
                )
        );
    }
}
