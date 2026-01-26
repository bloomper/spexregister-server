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

import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDeniedException(final AccessDeniedException e, final WebRequest request) {
        return createProblemDetail(e, HttpStatus.FORBIDDEN, "Access denied", "problemDetail.%s".formatted(e.getClass().getName()), null, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAuthenticationException(final AuthenticationException e, final WebRequest request) {
        return createProblemDetail(e, HttpStatus.FORBIDDEN, "Authentication denied", "problemDetail.%s".formatted(e.getClass().getName()), null, request);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAuthorizationDeniedException(final AuthorizationDeniedException e, final WebRequest request) {
        return createProblemDetail(e, HttpStatus.FORBIDDEN, "Authentication denied", "problemDetail.%s".formatted(e.getClass().getName()), null, request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleObjectOptimisticLockingFailureException(final ObjectOptimisticLockingFailureException e, final WebRequest request) {
        return createProblemDetail(e, HttpStatus.CONFLICT, "Resource conflict", "problemDetail.%s".formatted(e.getClass().getName()), null, request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleOptimisticLockingFailureException(final OptimisticLockingFailureException e, final WebRequest request) {
        return createProblemDetail(e, HttpStatus.CONFLICT, "Resource conflict", "problemDetail.%s".formatted(e.getClass().getName()), null, request);
    }

    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleOptimisticLockException(final OptimisticLockException e, final WebRequest request) {
        return createProblemDetail(e, HttpStatus.CONFLICT, "Resource conflict", "problemDetail.%s".formatted(e.getClass().getName()), null, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException e,
                                                                  final HttpHeaders headers,
                                                                  final HttpStatusCode status,
                                                                  final WebRequest request) {
        final Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(error -> {
            final String fieldName = ((FieldError) error).getField();
            final String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        final ProblemDetail problemDetail = createProblemDetail(e, HttpStatus.BAD_REQUEST, "Invalid input", "problemDetail.%s".formatted(e.getClass().getName()), null, request);

        problemDetail.setProperties(Map.of("errors", errors));

        return new ResponseEntity<>(problemDetail, headers, HttpStatus.BAD_REQUEST);
    }

}
