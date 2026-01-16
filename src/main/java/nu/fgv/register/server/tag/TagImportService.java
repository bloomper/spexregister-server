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
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class TagImportService extends AbstractImportService<TagImpexDto> {

    private final TagService service;

    public TagImportService(final List<ImportEngine> engines, final TagService service) {
        super(engines);
        this.service = service;
    }

    @Override
    protected ImportResultDto processImport(final List<TagImpexDto> dtos, final Locale locale) {
        dtos.forEach(dto -> {
            // TODO
        });
        return ImportResultDto.builder().success(true).build();
    }

    @Override
    protected Class<TagImpexDto> getImpexDtoClass() {
        return TagImpexDto.class;
    }

    @Override
    protected Function<Long, Boolean> getExistenceChecker() {
        return id -> {
            try {
                service.findById(id);
                return true;
            } catch (final Exception e) {
                return false;
            }
        };
    }

}
