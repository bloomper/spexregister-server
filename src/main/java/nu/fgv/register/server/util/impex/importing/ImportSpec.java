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

package nu.fgv.register.server.util.impex.importing;

import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Builder
@Getter
public class ImportSpec {
    private final Class<?> clazz;
    @Builder.Default
    private final Map<String, Function<Object, Boolean>> existenceCheckers = Collections.emptyMap();
    @Nullable
    private final String name;
}

