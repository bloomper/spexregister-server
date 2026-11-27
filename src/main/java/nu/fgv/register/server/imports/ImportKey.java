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

package nu.fgv.register.server.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * What a source key made, and what it looked like right after the import.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "import_key")
@Getter
@Setter
public class ImportKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_key", nullable = false, updatable = false)
    private String sourceKey;

    @Column(name = "source_hash", nullable = false, updatable = false)
    private String sourceHash;

    @Column(name = "record_kind", nullable = false, updatable = false)
    private String recordKind;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "imported_hash", nullable = false)
    private String importedHash;

    @Column(name = "record_version")
    private @Nullable Long recordVersion;

    @Column(name = "imported_by", nullable = false)
    private String importedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
