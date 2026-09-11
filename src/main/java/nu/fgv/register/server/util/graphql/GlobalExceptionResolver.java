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

package nu.fgv.register.server.util.graphql;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.ExportException;
import nu.fgv.register.server.util.error.ExternalResourceNotFoundException;
import nu.fgv.register.server.util.error.ImportException;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.JobDeleteNotAllowedException;
import nu.fgv.register.server.util.error.ResourceAlreadyExistsException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class GlobalExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private final MessageSource messageSource;

    public GlobalExceptionResolver(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    protected List<GraphQLError> resolveToMultipleErrors(final Throwable ex, final DataFetchingEnvironment environment) {
        if (ex instanceof final ConstraintViolationException e) {
            return e.getConstraintViolations().stream()
                    .map(error -> buildGraphqlError(ErrorType.BAD_REQUEST, e, environment))
                    .toList();
        } else {
            final GraphQLError error = resolveToSingleError(ex, environment);

            return error != null ? List.of(error) : Collections.emptyList();
        }
    }

    @Nullable
    @Override
    protected GraphQLError resolveToSingleError(final Throwable ex, final DataFetchingEnvironment environment) {
        return switch (ex) {
            case final AuthorizationDeniedException e -> buildGraphqlError(ErrorType.FORBIDDEN, e, environment);
            case final AccessDeniedException e -> buildGraphqlError(ErrorType.FORBIDDEN, e, environment);
            case final AuthenticationException e -> buildGraphqlError(ErrorType.FORBIDDEN, e, environment);
            case final BadRequestException e -> buildGraphqlError(ErrorType.BAD_REQUEST, e, environment);
            case final ExportException e -> buildGraphqlError(ErrorType.INTERNAL_ERROR, e, environment);
            case final ExternalResourceNotFoundException e -> buildGraphqlError(ErrorType.NOT_FOUND, e, environment);
            case final ImportException e -> buildGraphqlError(ErrorType.INTERNAL_ERROR, e, environment);
            case final InternalErrorException e -> buildGraphqlError(ErrorType.INTERNAL_ERROR, e, environment);
            case final JobDeleteNotAllowedException e -> buildGraphqlError(CustomErrorType.CONFLICT, e, environment);
            case final ResourceAlreadyExistsException e -> buildGraphqlError(CustomErrorType.CONFLICT, e, environment);
            case final ResourceNotFoundException e -> buildGraphqlError(ErrorType.NOT_FOUND, e, environment);
            case final ResourceNoValueException e -> buildGraphqlError(ErrorType.NOT_FOUND, e, environment);
            case final ResourcesNotFoundException e -> buildGraphqlError(ErrorType.NOT_FOUND, e, environment);
            case final SubresourceAlreadyExistsException e ->
                    buildGraphqlError(CustomErrorType.CONFLICT, e, environment);
            case final ObjectOptimisticLockingFailureException e ->
                    buildGraphqlError(CustomErrorType.CONFLICT, e, environment);
            case final OptimisticLockingFailureException e ->
                    buildGraphqlError(CustomErrorType.CONFLICT, e, environment);
            case final OptimisticLockException e -> buildGraphqlError(CustomErrorType.CONFLICT, e, environment);
            default -> null;
        };
    }

    private GraphQLError buildGraphqlError(final ErrorClassification errorType, final Throwable ex, final DataFetchingEnvironment environment) {
        if (ex instanceof final ResponseStatusException e) {
            final ProblemDetail problemDetail = e.updateAndGetBody(messageSource, LocaleContextHolder.getLocale());
            final String message = messageSource.getMessage(e.getDetailMessageCode(), e.getDetailMessageArguments(), e.getMessage(), LocaleContextHolder.getLocale());

            return GraphQLError.newError()
                    .errorType(errorType)
                    .message(message)
                    .path(environment.getExecutionStepInfo().getPath())
                    .location(environment.getField().getSourceLocation())
                    .extensions(Map.of(
                            "type", problemDetail.getType(),
                            "title", problemDetail.getTitle() != null ? problemDetail.getTitle() : "",
                            "status", problemDetail.getStatus(),
                            "detail", message != null ? message : "",
                            "instance", environment.getExecutionStepInfo().getPath().toString()
                    ))
                    .build();
        } else {
            final String message = messageSource.getMessage("problemDetail.%s".formatted(ex.getClass().getName()), null, ex.getMessage(), LocaleContextHolder.getLocale());

            return GraphQLError.newError()
                    .errorType(errorType)
                    .message(message)
                    .path(environment.getExecutionStepInfo().getPath())
                    .location(environment.getField().getSourceLocation())
                    .build();
        }
    }

}
