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
package nu.fgv.register.server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+[^}]*}");
    private static final Pattern LONE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");

    /**
     * Spring formats a message with MessageFormat only when it gets arguments, where an apostrophe quotes and must be
     * doubled; without arguments the text is used as it is, where a doubled apostrophe would show twice.
     */
    @ParameterizedTest
    @ValueSource(strings = {"messages_sv.properties", "messages_en.properties"})
    void apostrophesAreWrittenForHowTheMessageIsFormatted(final String file) throws IOException {
        final var messages = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(file)) {
            assertThat(in).as(file).isNotNull();
            messages.load(in);
        }

        messages.forEach((key, value) -> {
            final var text = (String) value;
            if (PLACEHOLDER.matcher(text).find()) {
                assertThat(LONE_APOSTROPHE.matcher(text).find()).as("%s in %s needs '' for an apostrophe", key, file).isFalse();
            } else {
                assertThat(text).as("%s in %s needs a single apostrophe", key, file).doesNotContain("''");
            }
        });
    }

    @Test
    void anApostropheBesideAnArgumentIsShown() {
        final var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");

        assertThat(source.getMessage("impex.import.validation.columnMismatch", new Object[]{2, "Name"}, Locale.ENGLISH))
                .isEqualTo("Expected column header 2 to be 'Name'");
    }
}
