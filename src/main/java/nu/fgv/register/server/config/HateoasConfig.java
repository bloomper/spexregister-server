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

import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.util.search.PagedWithFacetsResourcesAssembler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Configuration
public class HateoasConfig {

    private final Lazy<HateoasPageableHandlerMethodArgumentResolver> pageableResolver;

    public HateoasConfig(final ApplicationContext context) {
        this.pageableResolver = Lazy.of(() -> context.getBean("pageableResolver", HateoasPageableHandlerMethodArgumentResolver.class));
    }

    @Bean
    public PagedWithFacetsResourcesAssembler<SpexareDto> pagedWithFacetsResourcesAssembler() {
        return new PagedWithFacetsResourcesAssembler<>(pageableResolver.get(), null);
    }
}
