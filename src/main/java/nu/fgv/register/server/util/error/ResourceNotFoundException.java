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

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class ResourceNotFoundException extends ResponseStatusException {

    public ResourceNotFoundException(final String resourceType, final Object resourceIdentifier) {
        super(HttpStatus.NOT_FOUND, null, null,
                ErrorResponse.getDefaultDetailMessageCode(ResourceNotFoundException.class, resourceType),
                new Object[]{resourceIdentifier}
        );
    }

    public ResourceNotFoundException(final Class<?> resourceType, final Object resourceIdentifier) {
        this(resourceType.getSimpleName(), resourceIdentifier);
    }

    public ResourceNotFoundException(final String reason) {
        super(HttpStatus.NOT_FOUND, reason, null);
    }

}
