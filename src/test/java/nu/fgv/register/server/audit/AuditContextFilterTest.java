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

package nu.fgv.register.server.audit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AuditContextFilterTest {

    @Test
    void should_decode_a_percent_encoded_reason() throws Exception {
        assertThat(reasonRecordedFor("Massåtg%C3%A4rd%20%E2%80%93%20taggar"), is(equalTo("Massåtgärd – taggar")));
    }

    @Test
    void should_keep_a_raw_reason_as_is() throws Exception {
        assertThat(reasonRecordedFor("Fixed 100% of typos"), is(equalTo("Fixed 100% of typos")));
    }

    private static String reasonRecordedFor(final String header) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/graphql");
        final AtomicReference<String> recorded = new AtomicReference<>();

        request.addHeader(AuditContextFilter.REASON_HEADER, header);
        new AuditContextFilter().doFilter(request, new MockHttpServletResponse(), (_, _) -> recorded.set(AuditContext.current().comment()));

        return recorded.get();
    }
}
