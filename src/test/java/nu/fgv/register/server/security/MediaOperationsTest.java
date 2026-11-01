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

package nu.fgv.register.server.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code MEDIA} role of Spexify's and Spexflix's service accounts reads spex and nothing else.
 *
 * @author Anders Jacobsson
 * @since 2.1
 */
class MediaOperationsTest {

    private static final String BASE_PACKAGE = "nu.fgv.register.server";
    private static final String MEDIA = "'spexregister_MEDIA'";

    private static Set<String> graphqlOperationsOpenToMedia() {
        return controllers().stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(MediaOperationsTest::isOpenToMedia)
                .map(MediaOperationsTest::graphqlOperationOf)
                .filter(operation -> !operation.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> restControllersOpenToMedia() {
        return controllers().stream()
                .filter(type -> Arrays.stream(type.getDeclaredMethods())
                        .anyMatch(method -> AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) && isOpenToMedia(method)))
                .map(Class::getSimpleName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean isOpenToMedia(final Method method) {
        final PreAuthorize preAuthorize = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);

        return preAuthorize != null && preAuthorize.value().contains(MEDIA);
    }

    private static String graphqlOperationOf(final Method method) {
        final QueryMapping query = method.getAnnotation(QueryMapping.class);
        if (query != null) {
            return "Query." + (query.value().isEmpty() ? method.getName() : query.value());
        }
        final MutationMapping mutation = method.getAnnotation(MutationMapping.class);
        if (mutation != null) {
            return "Mutation." + (mutation.value().isEmpty() ? method.getName() : mutation.value());
        }
        final SchemaMapping schema = method.getAnnotation(SchemaMapping.class);
        if (schema != null) {
            return schema.typeName() + "." + (schema.field().isEmpty() ? method.getName() : schema.field());
        }
        final BatchMapping batch = method.getAnnotation(BatchMapping.class);
        if (batch != null) {
            return batch.typeName() + "." + (batch.field().isEmpty() ? method.getName() : batch.field());
        }
        return "";
    }

    private static Set<Class<?>> controllers() {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .map(definition -> ClassUtils.resolveClassName(definition.getBeanClassName(), MediaOperationsTest.class.getClassLoader()))
                .collect(Collectors.toSet());
    }

    @Test
    void should_let_media_through_to_exactly_reading_spex() {
        assertThat(graphqlOperationsOpenToMedia()).containsExactlyInAnyOrder(
                "Query.spexPaged", "Query.spex", "Query.spexRevival", "Spex.revivalsPaged", "Spex.revivals",
                "Spex.parent", "Spex.category");
    }

    @Test
    void should_let_media_through_to_no_rest_operation_but_reading_spexes() {
        assertThat(restControllersOpenToMedia()).containsExactly("SpexApi");
    }
}
