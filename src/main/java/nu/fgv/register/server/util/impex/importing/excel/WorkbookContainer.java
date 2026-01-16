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

package nu.fgv.register.server.util.impex.importing.excel;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.impex.util.excel.AbstractWorkbookContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
class WorkbookContainer extends AbstractWorkbookContainer {
    private final List<String> messages = new ArrayList<>();
    private Class<?> createClazz;
    private Class<?> updateClazz;
    private Function<Long, Boolean> existenceChecker;
    private final Validator validator;

    WorkbookContainer() {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }
}
