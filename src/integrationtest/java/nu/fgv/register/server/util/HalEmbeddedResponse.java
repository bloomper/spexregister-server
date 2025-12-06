/*
 * Copyright 2025 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class HalEmbeddedResponse<T> {

    @JsonProperty("_embedded")
    private Map<String, List<T>> embedded;

    public List<T> getList(final String key) {
        if (embedded == null) {
            return Collections.emptyList();
        }
        return embedded.getOrDefault(key, Collections.emptyList());
    }
}
