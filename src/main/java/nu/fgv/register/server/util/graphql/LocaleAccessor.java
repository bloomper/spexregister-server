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

import io.micrometer.context.ThreadLocalAccessor;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class LocaleAccessor implements ThreadLocalAccessor<Locale> {

    @Override
    public Object key() {
        return LocaleAccessor.class.getName();
    }

    @Override
    public Locale getValue() {
        return LocaleContextHolder.getLocale();
    }

    @Override
    public void setValue(final Locale locale) {
        LocaleContextHolder.setLocale(locale);
    }

}
