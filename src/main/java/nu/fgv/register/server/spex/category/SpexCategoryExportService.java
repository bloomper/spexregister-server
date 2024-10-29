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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExcelWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SpexCategoryExportService extends AbstractExportService {

    private final SpexCategoryService service;
    private final MessageSource messageSource;
    private final ExcelWriter writer = new ExcelWriter();

    protected byte[] doExport(final Workbook workbook, final List<Long> ids, final Locale locale) throws IOException {
        final var dtos = retrieveDtos(ids);

        writer.createSheet(messageSource, locale, workbook, dtos);
        return convertWorkbookToByteArray(workbook);
    }

    private List<SpexCategoryDto> retrieveDtos(final List<Long> ids) {
        if (ids.isEmpty()) {
            return service.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else {
            return service.findByIds(ids, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
    }

}
