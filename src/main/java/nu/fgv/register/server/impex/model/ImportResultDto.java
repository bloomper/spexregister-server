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

package nu.fgv.register.server.impex.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Builder
@ToString
public class ImportResultDto {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("errors")
    private List<String> errors;

    @JsonProperty("messages")
    private List<String> messages;

    @JsonProperty("data")
    @Nullable
    private Map<String, Object> data;
}
