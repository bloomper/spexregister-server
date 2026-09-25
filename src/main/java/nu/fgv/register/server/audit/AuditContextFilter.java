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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class AuditContextFilter extends OncePerRequestFilter {

    static final String REASON_HEADER = "X-Audit-Reason";

    private static @Nullable String operationOf(final HttpServletRequest request) {
        final String uri = request.getRequestURI();

        return uri == null ? null : "%s %s".formatted(request.getMethod(), uri);
    }

    private static @Nullable String reasonOf(final HttpServletRequest request) {
        final String reason = request.getHeader(REASON_HEADER);

        if (reason == null) {
            return null;
        }

        try {
            return UriUtils.decode(reason, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException _) {
            return reason;
        }
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        AuditContext.set(new AuditContext.Origin(AuditSource.WEB, operationOf(request), reasonOf(request)));

        try {
            chain.doFilter(request, response);
        } finally {
            AuditContext.clear();
        }
    }
}
