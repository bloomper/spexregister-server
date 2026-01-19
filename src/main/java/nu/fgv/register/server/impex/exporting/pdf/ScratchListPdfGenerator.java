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

package nu.fgv.register.server.impex.exporting.pdf;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.spexare.SpexareImpexDto;
import nu.fgv.register.server.util.error.ExportException;
import nu.fgv.register.server.impex.exporting.ExportHolder;
import nu.fgv.register.server.impex.model.ExportType;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Component
public class ScratchListPdfGenerator implements PdfGenerator {

    private final MessageSource messageSource;

    @SuppressWarnings("unchecked")
    @Override
    public byte[] generate(final List<ExportHolder<?>> reports, final Locale locale) {
        ExportHolder<SpexareImpexDto> spexareReport = null;

        for (final ExportHolder<?> report : reports) {
            if (report.clazz().equals(SpexareImpexDto.class)) {
                spexareReport = (ExportHolder<SpexareImpexDto>) report;
            }
        }

        if (spexareReport == null) {
            throw new ExportException("Required data missing");
        }

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final Document document = new Document(PageSize.A4, 20, 20, 10, 10);
            final PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            document.open();

            final float[] columnWidths = {195f, 120f, 120f, 120f};
            final PdfPTable table = new PdfPTable(columnWidths);

            table.setWidthPercentage(100);

            final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            final Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            final Color lightGray = new Color(204, 204, 204);

            final String fullNameHeader = messageSource.getMessage("impex.export.pdf.scratchList.fullName", null, "impex.export.pdf.scratchList.fullName", locale);
            final PdfPCell headerSpan = new PdfPCell(new Phrase(""));

            headerSpan.setColspan(4);
            headerSpan.setBackgroundColor(lightGray);
            headerSpan.setMinimumHeight(30f);
            table.addCell(headerSpan);

            table.addCell(createCell(fullNameHeader, headerFont, lightGray, 30f, Element.ALIGN_LEFT));
            table.addCell(createCell("", normalFont, lightGray, 30f, Element.ALIGN_CENTER));
            table.addCell(createCell("", normalFont, lightGray, 30f, Element.ALIGN_CENTER));
            table.addCell(createCell("", normalFont, lightGray, 30f, Element.ALIGN_CENTER));

            table.setHeaderRows(2);

            int rowCount = 0;
            for (final SpexareImpexDto spexare : spexareReport.data()) {
                final Color bgColor = (rowCount % 2 != 0) ? lightGray : Color.WHITE;
                final String fullName = spexare.getFirstName() + " " + spexare.getLastName();

                table.addCell(createCell(fullName, normalFont, bgColor, 20f, Element.ALIGN_LEFT));
                table.addCell(createCell("", normalFont, bgColor, 20f, Element.ALIGN_CENTER));
                table.addCell(createCell("", normalFont, bgColor, 20f, Element.ALIGN_CENTER));
                table.addCell(createCell("", normalFont, bgColor, 20f, Element.ALIGN_CENTER));

                rowCount++;
            }

            document.add(table);
            document.close();
            writer.close();

            return outputStream.toByteArray();
        } catch (final Exception e) {
            throw new ExportException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(final ExportType type) {
        return type == ExportType.PDF_SCRATCH_LIST;
    }

    private PdfPCell createCell(final String text, final Font font, final Color bgColor, final float height, final int alignment) {
        final PdfPCell cell = new PdfPCell(new Phrase(text, font));

        cell.setBackgroundColor(bgColor);
        cell.setMinimumHeight(height);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(alignment);
        cell.setPaddingLeft(5f);

        return cell;
    }

}
