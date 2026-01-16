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

package nu.fgv.register.server.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.util.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import nu.fgv.register.server.util.impex.model.excel.ExcelSheet;

import java.time.LocalDate;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "news.impex.sheetName")
public class NewsImpexDto extends AbstractAuditableImpexDto<NewsImpexDto> {

    @JsonProperty("id")
    @ExcelCell(header = "news.impex.id.columnName", position = 1, primaryKey = true)
    private Long id;

    @NotBlank(message = "{news.subject.notEmpty}")
    @Size(max = 255, message = "{news.subject.size}")
    @JsonProperty("subject")
    @ExcelCell(header = "news.impex.subject.columnName", position = 2, updatable = true, mandatory = true)
    private String subject;

    @NotBlank(message = "{news.text.notEmpty}")
    @Size(max = 10000, message = "{news.text.size}")
    @JsonProperty("text")
    @ExcelCell(header = "news.impex.text.columnName", position = 3, updatable = true, mandatory = true)
    private String text;

    @JsonProperty("visibleFrom")
    @ExcelCell(header = "news.impex.visibleFrom.columnName", position = 4, updatable = true)
    private LocalDate visibleFrom;

    @JsonProperty("visibleTo")
    @ExcelCell(header = "news.impex.visibleTo.columnName", position = 5, updatable = true)
    private LocalDate visibleTo;

    @JsonProperty("published")
    @ExcelCell(header = "news.impex.published.columnName", position = 6)
    private Boolean published;

}
