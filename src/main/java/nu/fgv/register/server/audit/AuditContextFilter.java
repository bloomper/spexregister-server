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

import java.io.IOException;

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

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        AuditContext.set(new AuditContext.Origin(AuditSource.WEB, operationOf(request), request.getHeader(REASON_HEADER)));

        try {
            chain.doFilter(request, response);
        } finally {
            AuditContext.clear();
        }
    }
}
