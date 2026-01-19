/*
 * Copyright 2024 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.impex.model.excel.ExcelCell;
import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractAuditableDto<T extends RepresentationModel<? extends T>> extends RepresentationModel<T> {

    @JsonProperty("createdBy")
    @ExcelCell(header = "common.impex.createdBy.columnName", position = 0)
    private String createdBy;

    @JsonProperty("createdAt")
    @ExcelCell(header = "common.impex.createdAt.columnName", position = 1)
    private Instant createdAt;

    @JsonProperty("lastModifiedBy")
    @ExcelCell(header = "common.impex.lastModifiedBy.columnName", position = 2)
    private String lastModifiedBy;

    @JsonProperty("lastModifiedAt")
    @ExcelCell(header = "common.impex.lastModifiedAt.columnName", position = 3)
    private Instant lastModifiedAt;

}
