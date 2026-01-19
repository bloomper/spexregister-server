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

package nu.fgv.register.server.util.graphql;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import nu.fgv.register.server.util.error.ExportException;
import nu.fgv.register.server.util.error.ImportException;
import nu.fgv.register.server.util.error.InternalErrorException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.SubscriptionExceptionResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.util.Collections.singletonList;
import static reactor.core.publisher.Mono.just;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class GlobalSubscriptionExceptionResolver implements SubscriptionExceptionResolver {

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private final MessageSource messageSource;

    public GlobalSubscriptionExceptionResolver(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Mono<List<GraphQLError>> resolveException(final Throwable exception) {
        return switch (exception) {
            case final AuthenticationException e -> just(singletonList(buildError(ErrorType.UNAUTHORIZED, e)));
            case final AccessDeniedException e -> ReactiveSecurityContextHolder.getContext()
                    .map(context -> singletonList(
                            trustResolver.isAnonymous(context.getAuthentication())
                                    ? buildError(ErrorType.UNAUTHORIZED, e)
                                    : buildError(ErrorType.FORBIDDEN, e)
                    ))
                    .switchIfEmpty(just(singletonList(buildError(ErrorType.UNAUTHORIZED, e))));
            case final ExportException e -> just(singletonList(buildError(ErrorType.INTERNAL_ERROR, e)));
            case final ImportException e -> just(singletonList(buildError(ErrorType.INTERNAL_ERROR, e)));
            case final InternalErrorException e -> just(singletonList(buildError(ErrorType.INTERNAL_ERROR, e)));
            default -> Mono.empty();
        };
    }

    private GraphQLError buildError(final ErrorClassification errorType, final Throwable ex) {
        final String message = messageSource.getMessage("problemDetail.%s".formatted(ex.getClass().getName()), null, ex.getMessage(), LocaleContextHolder.getLocale());

        return GraphqlErrorBuilder.newError()
                .errorType(errorType)
                .message(message)
                .build();
    }
}
