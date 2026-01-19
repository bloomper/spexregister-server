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
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.util.error.ExportException;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Component
public class AddressLabelsPdfGenerator implements PdfGenerator {

    private final MessageSource messageSource;

    @SuppressWarnings("unchecked")
    @Override
    public byte[] generate(final List<ExportHolder<?>> reports, final Locale locale) {
        ExportHolder<SpexareImpexDto> spexareReport = null;
        ExportHolder<AddressImpexDto> addressReport = null;

        for (final ExportHolder<?> report : reports) {
            if (report.clazz().equals(SpexareImpexDto.class)) {
                spexareReport = (ExportHolder<SpexareImpexDto>) report;
            }
            if (report.clazz().equals(AddressImpexDto.class)) {
                addressReport = (ExportHolder<AddressImpexDto>) report;
            }
        }

        if (spexareReport == null || addressReport == null) {
            throw new ExportException("Required data missing");
        }

        final Map<Long, SpexareImpexDto> spexareMap = new HashMap<>();

        spexareReport.data().forEach(s -> spexareMap.put(s.getId(), s));

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final Document document = new Document(PageSize.A4, 0, 0, 9, 9);
            final PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            document.open();

            final PdfPTable table = new PdfPTable(3);

            table.setWidthPercentage(100);
            table.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

            final Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            int labelsAdded = 0;

            for (final AddressImpexDto address : addressReport.data()) {
                final SpexareImpexDto spexare = spexareMap.get(address.getSpexareId());

                if (spexare != null && "HOME".equalsIgnoreCase(address.getTypeId())) {
                    final PdfPCell cell = new PdfPCell();

                    cell.setBorder(PdfPCell.NO_BORDER);
                    cell.setMinimumHeight(103f);
                    cell.setPaddingLeft(15f);
                    cell.setPaddingRight(0f);
                    cell.setPaddingTop(0f);
                    cell.setPaddingBottom(0f);
                    cell.setVerticalAlignment(Element.ALIGN_TOP);

                    final Paragraph p = new Paragraph();

                    p.setFont(boldFont);
                    p.setLeading(12f);

                    p.add(spexare.getFirstName() + " " + spexare.getLastName() + "\n");
                    p.add((hasText(address.getStreetAddress()) ? address.getStreetAddress() : "") + "\n");

                    final String postalCode = hasText(address.getPostalCode()) ? address.getPostalCode() + " " : "";
                    final String city = hasText(address.getCity()) ? address.getCity() : "";

                    p.add(postalCode + city + "\n");

                    if (hasText(address.getCountryName())) {
                        p.add(address.getCountryName().toUpperCase());
                    }

                    cell.addElement(p);
                    table.addCell(cell);
                    labelsAdded++;
                }
            }

            if (labelsAdded == 0) {
                final String noAddressesFound = messageSource.getMessage("impex.export.pdf.addressLabels.noHomeAddressesFound", null, "impex.export.pdf.addressLabels.noHomeAddressesFound", locale);

                document.add(new Paragraph(noAddressesFound));
            } else {
                table.completeRow();
                document.add(table);
            }

            document.close();
            writer.close();

            return outputStream.toByteArray();
        } catch (final Exception e) {
            throw new ExportException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(final ReportType type) {
        return type == ReportType.PDF_ADDRESS_LABELS;
    }

}
