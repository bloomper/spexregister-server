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

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class CustomScalars {

    public static final GraphQLScalarType Instant = GraphQLScalarType.newScalar()
            .name("Instant")
            .description("A custom scalar that handles Java 8 Instant types")
            .coercing(new Coercing<Instant, String>() {
                @Override
                public String serialize(final Object dataFetcherResult, final GraphQLContext graphQLContext, final Locale locale) throws CoercingSerializeException {
                    try {
                        final Instant publishedTime = (Instant) dataFetcherResult;

                        return DateTimeFormatter.ISO_INSTANT.format(publishedTime.atZone(ZoneId.of("UTC")));
                    } catch (final CoercingSerializeException e) {
                        throw new CoercingSerializeException("Invalid input", e);
                    }
                }

                @Override
                public Instant parseValue(final Object input, final GraphQLContext graphQLContext, final Locale locale) throws CoercingParseValueException {
                    try {
                        return LocalDate.parse((String) input).atStartOfDay(ZoneId.of("UTC")).toInstant();
                    } catch (final RuntimeException e) {
                        throw new CoercingParseValueException("Invalid input", e);
                    }
                }

                @Override
                public Instant parseLiteral(final Value input, final CoercedVariables variables, final GraphQLContext graphQLContext, final Locale locale) throws CoercingParseLiteralException {
                    try {
                        final StringValue stringValue = (StringValue) input;
                        final LocalDate date = LocalDate.parse(stringValue.getValue());

                        return date.atStartOfDay(ZoneId.of("UTC")).toInstant();
                    } catch (final RuntimeException e) {
                        throw new CoercingParseLiteralException("Invalid input", e);
                    }
                }
            })
            .build();

    public static final GraphQLScalarType Void = GraphQLScalarType.newScalar()
            .name("Void")
            .description("A custom scalar that represents the null value")
            .coercing(new Coercing<Void, Void>() {
                @Override
                public Void serialize(final Object dataFetcherResult, final GraphQLContext graphQLContext, final Locale locale) throws CoercingSerializeException {
                    return null;
                }

                @Override
                public Void parseValue(final Object input, final GraphQLContext graphQLContext, final Locale locale) throws CoercingParseValueException {
                   return null;
                }

                @Override
                public Void parseLiteral(final Value input, final CoercedVariables variables, final GraphQLContext graphQLContext, final Locale locale) throws CoercingParseLiteralException {
                    return null;
                }
            })
            .build();
}
