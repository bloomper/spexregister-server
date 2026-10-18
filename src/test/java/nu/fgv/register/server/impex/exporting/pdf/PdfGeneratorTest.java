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

import nu.fgv.register.server.impex.exporting.ExportHolder;
import nu.fgv.register.server.impex.model.ReportType;
import nu.fgv.register.server.spexare.SpexareImpexDto;
import nu.fgv.register.server.spexare.activity.ActivityImpexDto;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.util.error.ExportException;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.context.support.StaticMessageSource;

import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class PdfGeneratorTest {

    private static final Locale LOCALE = Locale.ENGLISH;

    private final StaticMessageSource messageSource = new StaticMessageSource();

    @Test
    void should_support_only_their_own_report_type() {
        assertThat(new AddressLabelsPdfGenerator(messageSource).supports(ReportType.PDF_ADDRESS_LABELS), is(true));
        assertThat(new AddressLabelsPdfGenerator(messageSource).supports(ReportType.PDF_SCRATCH_LIST), is(false));
        assertThat(new ScratchListPdfGenerator(messageSource).supports(ReportType.PDF_SCRATCH_LIST), is(true));
        assertThat(new PlatoonListPdfGenerator(messageSource).supports(ReportType.PDF_PLATOON_LIST), is(true));
    }

    @Test
    void should_label_only_home_addresses() throws Exception {
        final byte[] pdf = new AddressLabelsPdfGenerator(messageSource).generate(List.of(
                ExportHolder.of(List.of(spexare(1L, "Ada", "Lovelace"), spexare(2L, "Grace", "Hopper")), SpexareImpexDto.class),
                ExportHolder.of(List.of(address(1L, "HOME", "Storgatan 1"), address(2L, "WORK", "Kontorsvägen 2")), AddressImpexDto.class)
        ), LOCALE);

        final String text = text(pdf);

        assertThat(text, containsString("Ada Lovelace"));
        assertThat(text, containsString("Storgatan 1"));
        assertThat(text, containsString("SWEDEN"));
        assertThat(text, not(containsString("Grace Hopper")));
        assertThat(text, not(containsString("Kontorsvägen 2")));
    }

    @Test
    void should_say_so_when_there_are_no_home_addresses() throws Exception {
        messageSource.addMessage("impex.export.pdf.addressLabels.noHomeAddressesFound", LOCALE, "No home addresses");

        final byte[] pdf = new AddressLabelsPdfGenerator(messageSource).generate(List.of(
                ExportHolder.of(List.of(spexare(1L, "Ada", "Lovelace")), SpexareImpexDto.class),
                ExportHolder.of(List.<AddressImpexDto>of(), AddressImpexDto.class)
        ), LOCALE);

        assertThat(text(pdf), containsString("No home addresses"));
    }

    @Test
    void should_list_every_spexare_on_the_scratch_list() throws Exception {
        final byte[] pdf = new ScratchListPdfGenerator(messageSource).generate(List.of(
                ExportHolder.of(List.of(spexare(1L, "Ada", "Lovelace"), spexare(2L, "Grace", "Hopper")), SpexareImpexDto.class)
        ), LOCALE);

        final String text = text(pdf);

        assertThat(text, containsString("Ada Lovelace"));
        assertThat(text, containsString("Grace Hopper"));
    }

    @Test
    void should_combine_role_and_home_address_on_the_platoon_list() throws Exception {
        final byte[] pdf = new PlatoonListPdfGenerator(messageSource).generate(List.of(
                ExportHolder.of(List.of(spexare(1L, "Ada", "Lovelace"), spexare(2L, "Grace", "Hopper")), SpexareImpexDto.class),
                ExportHolder.of(List.of(address(1L, "HOME", "Storgatan 1")), AddressImpexDto.class),
                ExportHolder.of(List.of(activity(1L, "Orchestra")), ActivityImpexDto.class)
        ), LOCALE);

        final String text = text(pdf);

        assertThat(text, containsString("Orchestra"));
        assertThat(text, containsString("Ada Lovelace"));
        assertThat(text, containsString("Storgatan 1"));
        assertThat(text, containsString("Grace Hopper"));
    }

    @Test
    void should_refuse_to_generate_without_the_data_it_needs() {
        final List<ExportHolder<?>> spexareOnly = List.of(ExportHolder.of(List.of(spexare(1L, "Ada", "Lovelace")), SpexareImpexDto.class));

        assertThrows(ExportException.class, () -> new AddressLabelsPdfGenerator(messageSource).generate(spexareOnly, LOCALE));
        assertThrows(ExportException.class, () -> new PlatoonListPdfGenerator(messageSource).generate(spexareOnly, LOCALE));
        assertThrows(ExportException.class, () -> new ScratchListPdfGenerator(messageSource).generate(List.of(), LOCALE));
    }

    private static String text(final byte[] pdf) throws Exception {
        try (final PdfReader reader = new PdfReader(pdf)) {
            final PdfTextExtractor extractor = new PdfTextExtractor(reader);
            final StringBuilder text = new StringBuilder();

            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        }
    }

    private static SpexareImpexDto spexare(final Long id, final String firstName, final String lastName) {
        final SpexareImpexDto dto = new SpexareImpexDto();

        dto.setId(id);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        return dto;
    }

    private static AddressImpexDto address(final Long spexareId, final String typeId, final String streetAddress) {
        final AddressImpexDto dto = new AddressImpexDto();

        dto.setSpexareId(spexareId);
        dto.setTypeId(typeId);
        dto.setStreetAddress(streetAddress);
        dto.setPostalCode("411 01");
        dto.setCity("Göteborg");
        dto.setCountryName("Sweden");
        return dto;
    }

    private static ActivityImpexDto activity(final Long spexareId, final String taskName) {
        final ActivityImpexDto dto = new ActivityImpexDto();

        dto.setSpexareId(spexareId);
        dto.setTaskName(taskName);
        return dto;
    }
}
