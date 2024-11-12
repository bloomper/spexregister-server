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

package nu.fgv.register.server.util.impex.exporting;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.error.ExportException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
public abstract class AbstractExportService {

    public Pair<String, byte[]> doExport(final List<Long> ids, final String type, final Locale locale) {
        final Workbook workbook;
        final String extension;
        switch (type) {
            case Constants.MediaTypes.APPLICATION_XLSX_VALUE -> {
                workbook = new XSSFWorkbook();
                extension = ".xlsx";
            }
            case Constants.MediaTypes.APPLICATION_XLS_VALUE -> {
                workbook = new HSSFWorkbook();
                extension = ".xls";
            }
            default -> throw new IllegalArgumentException("Unrecognized type");
        }
        return Pair.of(extension, doExport(workbook, ids, locale));
    }

    protected abstract byte[] doExport(final Workbook workbook, final List<Long> ids, final Locale locale);

    protected byte[] convertWorkbookToByteArray(final Workbook workbook) {
        try {
            final var outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
            return outputStream.toByteArray();
        } catch (final IOException e) {
            log.error("Unexpected error while exporting", e);
            throw new ExportException(e.getMessage());
        }
    }
}
