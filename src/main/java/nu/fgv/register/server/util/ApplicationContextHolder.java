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

package nu.fgv.register.server.util;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * The application context, for code Spring does not create.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static @Nullable ApplicationContext context;

    /**
     * @param beanClass the bean's type
     * @param <T>       the bean's type
     * @return the bean
     */
    public static <T> T getBean(final Class<T> beanClass) {
        return Objects.requireNonNull(context, "Application context is not initialized").getBean(beanClass);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        context = applicationContext;
    }
}
