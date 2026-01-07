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

package nu.fgv.register.server.config;

import graphql.scalars.ExtendedScalars;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.util.graphql.CustomScalars;
import nu.fgv.register.server.util.graphql.CustomSortStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.data.query.SortStrategy;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.util.regex.Pattern;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Configuration
public class GraphqlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.CountryCode)
                .scalar(ExtendedScalars.Locale)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(
                        ExtendedScalars.newRegexScalar("Email")
                                .addPattern(Pattern.compile("^(.+)@(\\S+)$"))
                                .build()
                )
                .scalar(
                        ExtendedScalars.newRegexScalar("Year")
                                .addPattern(Pattern.compile("^(19|20|21)\\d{2}$"))
                                .build()
                )
                .scalar(
                        ExtendedScalars.newRegexScalar("SocialSecurityNumber")
                                .addPattern(Pattern.compile(Spexare.SOCIAL_SECURITY_NUMBER_PATTERN))
                                .build()
                )
                .scalar(CustomScalars.Instant)
                .scalar(CustomScalars.Void)
                .type("SpexareWithFacetsConnection", typeWiring -> typeWiring
                        .dataFetcher("facets", env ->
                                env.getGraphQlContext().getOrDefault("facets", null)));

    }

    @Bean
    public SortStrategy sortStrategy() {
        return new CustomSortStrategy();
    }

}
