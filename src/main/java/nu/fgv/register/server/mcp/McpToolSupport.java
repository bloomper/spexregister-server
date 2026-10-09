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

package nu.fgv.register.server.mcp;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.audit.AuditContext;
import nu.fgv.register.server.audit.AuditSource;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class McpToolSupport {

    static final String FILTER_DESCRIPTION = """
            Optional filter. Syntax: <field><op><value> where op is ':' (equals), '!' (not equals), \
            '>' (greater than), '<' (less than) or '~' (like); '*' is a wildcard in values, e.g. name~*foo*. \
            Values cannot contain spaces. Combine criteria with AND/OR and parentheses.""";
    static final String PAGE_DESCRIPTION = "Zero-based page number, defaults to 0";
    static final String SIZE_DESCRIPTION = "Page size, defaults to 25, max 100";

    static final int DEFAULT_PAGE_SIZE = 25;
    static final int MAX_PAGE_SIZE = 100;

    private final Validator validator;

    static Pageable pageable(final @Nullable Integer page, final @Nullable Integer size, final Sort sort) {
        final int p = page == null || page < 0 ? 0 : page;
        final int s = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        return PageRequest.of(p, s, sort);
    }

    static String filter(final @Nullable String filter) {
        return hasText(filter) ? filter.trim() : "";
    }

    static <T> T orElse(final @Nullable T value, final T fallback) {
        return value != null ? value : fallback;
    }

    void audit(final String tool) {
        AuditContext.stamp(AuditContext.currentOrSystem()
                .withSource(AuditSource.MCP)
                .withOperation("MCP %s".formatted(tool)));
    }

    <T> T validate(final T dto) {
        final Set<ConstraintViolation<T>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        return dto;
    }

    public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

        static <S, T> PageResult<T> of(final Page<S> page, final Function<S, T> mapper) {
            return new PageResult<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
