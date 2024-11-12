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
public class ResourcesNotFoundException extends ResponseStatusException {

    public ResourcesNotFoundException(final String[] resourceTypes,
                                      final Object resourceIdentifier,
                                      final Object... secondaryResourceIdentifiers) {
        super(HttpStatus.NOT_FOUND, null, null,
                ErrorResponse.getDefaultDetailMessageCode(ResourcesNotFoundException.class,
                        String.join("|", resourceTypes)),
                Stream.concat(
                        Stream.of(resourceIdentifier),
                        Stream.of(secondaryResourceIdentifiers)
                ).toArray(Object[]::new));
    }

    public ResourcesNotFoundException(final List<Class<?>> resourceTypes,
                                      final Object resourceIdentifier,
                                      final Object... secondaryResourceIdentifiers) {
        this(
                resourceTypes.stream().map(Class::getSimpleName).toArray(String[]::new),
                resourceIdentifier,
                secondaryResourceIdentifiers
        );
    }

    public ResourcesNotFoundException(final String reason) {
        super(HttpStatus.NOT_FOUND, reason, null);
    }

}
