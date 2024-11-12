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

package nu.fgv.register.server.util.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class SubresourceAlreadyExistsException extends ResponseStatusException {

    public SubresourceAlreadyExistsException(final String[] resourceTypes,
                                             final String duplicatedResourceAttribute,
                                             final Object duplicatedResourceValue,
                                             final Object resourceIdentifier,
                                             final Object... secondaryResourceIdentifiers) {
        super(HttpStatus.CONFLICT, null, null,
                ErrorResponse.getDefaultDetailMessageCode(ResourcesNotFoundException.class,
                        String.join("|", resourceTypes)),
                Stream.concat(
                        Stream.concat(
                                Stream.of(resourceIdentifier),
                                Stream.of(duplicatedResourceAttribute)
                        ),
                        Stream.concat(
                                Stream.of(duplicatedResourceValue),
                                Stream.of(secondaryResourceIdentifiers)
                        )
                ).toArray(Object[]::new));
    }

    public SubresourceAlreadyExistsException(final List<Class<?>> resourceTypes,
                                             final String duplicatedResourceAttribute,
                                             final Object duplicatedResourceValue,
                                             final Object resourceIdentifier,
                                             final Object... secondaryResourceIdentifiers) {
        this(
                resourceTypes.stream().map(Class::getSimpleName).toArray(String[]::new),
                duplicatedResourceAttribute,
                duplicatedResourceValue,
                resourceIdentifier,
                secondaryResourceIdentifiers
        );
    }

    public SubresourceAlreadyExistsException(final String reason) {
        super(HttpStatus.CONFLICT, reason, null);
    }

}
