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
import nu.fgv.register.server.impex.exporting.ExportHolder;
import nu.fgv.register.server.impex.model.ReportType;
import nu.fgv.register.server.spexare.SpexareImpexDto;
import nu.fgv.register.server.spexare.activity.ActivityImpexDto;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.util.error.ExportException;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Component
public class PlatoonListPdfGenerator implements PdfGenerator {

    private final MessageSource messageSource;

    @SuppressWarnings("unchecked")
    @Override
    public byte[] generate(final List<ExportHolder<?>> reports, final Locale locale) {
        ExportHolder<SpexareImpexDto> spexareReport = null;
        ExportHolder<AddressImpexDto> addressReport = null;
        ExportHolder<ActivityImpexDto> activityReport = null;

        for (final ExportHolder<?> report : reports) {
            if (report.clazz().equals(SpexareImpexDto.class)) {
                spexareReport = (ExportHolder<SpexareImpexDto>) report;
            } else if (report.clazz().equals(AddressImpexDto.class)) {
                addressReport = (ExportHolder<AddressImpexDto>) report;
            } else if (report.clazz().equals(ActivityImpexDto.class)) {
                activityReport = (ExportHolder<ActivityImpexDto>) report;
            }
        }

        if (spexareReport == null || addressReport == null || activityReport == null) {
            throw new ExportException("Required data missing");
        }

        final Map<Long, AddressImpexDto> addressMap = new HashMap<>();
        addressReport.data().forEach(a -> {
            if ("HOME".equalsIgnoreCase(a.getTypeId())) {
                addressMap.put(a.getSpexareId(), a);
            }
        });

        final Map<Long, String> roleMap = new HashMap<>();
        activityReport.data().forEach(activity -> roleMap.put(activity.getSpexareId(), activity.getTaskName()));

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final Document document = new Document(PageSize.A4, 10, 10, 10, 10);
            final PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            document.open();

            final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            final Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            addHeaderLine(document, messageSource.getMessage("impex.export.pdf.platoonList.title", null, "impex.export.pdf.platoonList.title", locale), titleFont);
            addHeaderLine(document, " ", subTitleFont);

            final float[] columnWidths = {65f, 100f, 60f, 140f, 145f, 65f};
            final PdfPTable table = new PdfPTable(columnWidths);
            table.setWidthPercentage(100);

            final Font detailFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            final Color lightGray = new Color(204, 204, 204);

            int rowCount = 0;
            for (final SpexareImpexDto spexare : spexareReport.data()) {
                final Color bgColor = (rowCount % 2 == 0) ? lightGray : Color.WHITE;
                final AddressImpexDto address = addressMap.get(spexare.getId());
                final String role = roleMap.getOrDefault(spexare.getId(), "");

                table.addCell(createCell(role, detailFont, bgColor, 15f, Element.ALIGN_LEFT));
                table.addCell(createCell(spexare.getFirstName() + " " + spexare.getLastName(), detailFont, bgColor, 15f, Element.ALIGN_LEFT));
                table.addCell(createCell(Optional.ofNullable(spexare.getSocialSecurityNumber()).orElse(""), detailFont, bgColor, 15f, Element.ALIGN_LEFT));
                table.addCell(createCell(address != null ? address.getStreetAddress() : "", detailFont, bgColor, 15f, Element.ALIGN_LEFT));

                final PdfPCell emailCell = createCell(address != null ? address.getEmailAddress() : "", detailFont, bgColor, 30f, Element.ALIGN_LEFT);

                emailCell.setRowspan(2);
                table.addCell(emailCell);
                table.addCell(createCell(address != null ? address.getPhoneMobile() : "", detailFont, bgColor, 15f, Element.ALIGN_LEFT));
                table.addCell(createCell("", detailFont, bgColor, 15f, Element.ALIGN_LEFT));
                table.addCell(createCell(Optional.ofNullable(spexare.getNickName()).orElse(""), detailFont, bgColor, 15f, Element.ALIGN_LEFT));
                table.addCell(createCell("", detailFont, bgColor, 15f, Element.ALIGN_LEFT));

                final String postal = address != null ? (hasText(address.getPostalCode()) ? address.getPostalCode() + " " : "") + (hasText(address.getCity()) ? address.getCity() : "") : "";
                table.addCell(createCell(postal, detailFont, bgColor, 15f, Element.ALIGN_LEFT));
                table.addCell(createCell(address != null ? address.getPhone() : "", detailFont, bgColor, 15f, Element.ALIGN_LEFT));

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
    public boolean supports(final ReportType type) {
        return type == ReportType.PDF_PLATOON_LIST;
    }

    private void addHeaderLine(final Document document, final String text, final Font font) {
        final Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
    }

    private PdfPCell createCell(final String text, final Font font, final Color bgColor, final float height, final int alignment) {
        final PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));

        cell.setBackgroundColor(bgColor);
        cell.setMinimumHeight(height);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingLeft(2f);

        return cell;
    }
}
