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

package nu.fgv.register.server.util.data;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
record AuditedTable(
        String entityName,
        String table,
        String auditTable,
        List<String> columns,
        List<String> mutations
) {

    static AuditedTable of(final String entityName, final String table, final String columns) {
        return new AuditedTable(entityName, table, table + "_audit", List.of(columns.split(",\\s*")), List.of());
    }

    AuditedTable withHistory(final String... mutations) {
        return new AuditedTable(entityName, table, auditTable, columns, List.of(mutations));
    }

    boolean hasHistory() {
        return !mutations.isEmpty();
    }

    String columnList() {
        return String.join(", ", columns);
    }
}