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

package nu.fgv.register.server.impex.exporting.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.impex.exporting.ExportEngine;
import nu.fgv.register.server.impex.exporting.ExportHolder;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.ReportType;
import nu.fgv.register.server.util.error.ExportException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ExcelExportEngine implements ExportEngine {

    private final MessageSource messageSource;
    private final ExcelWriter writer = new ExcelWriter();

    @Override
    public byte[] export(final List<ExportHolder<?>> reports, final ImpexType type, @Nullable final ReportType reportType, final Locale locale) {
        try (final Workbook workbook = createWorkbook(type)) {
            for (final ExportHolder<?> report : reports) {
                createSheetHelper(workbook, report, locale);
            }

            return convertToBytes(workbook);
        } catch (final IOException e) {
            throw new ExportException("Excel generation failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(final ImpexType type) {
        return type == ImpexType.EXCEL || type == ImpexType.EXCEL_XLS;
    }

    @Override
    public String getExtension(final ImpexType type) {
        return type == ImpexType.EXCEL ? ".xlsx" : ".xls";
    }

    private <T> void createSheetHelper(final Workbook workbook, final ExportHolder<T> report, final Locale locale) {
        writer.createSheet(messageSource, locale, workbook, report.data(), report.name(), report.clazz(), report.readOnly());
    }

    private Workbook createWorkbook(final ImpexType type) {
        if (type == ImpexType.EXCEL) {
            return new SXSSFWorkbook(100);
        }
        return new HSSFWorkbook();
    }

    private byte[] convertToBytes(final Workbook workbook) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        workbook.write(bos);

        return bos.toByteArray();
    }
}
