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

package nu.fgv.register.server.config;

import nu.fgv.register.server.spexare.SocialSecurityNumberHashBridge;
import nu.fgv.register.server.util.security.SocialSecurityNumberHasher;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Configuration
public class SearchConfig {

    @Bean
    public HibernatePropertiesCustomizer searchBeanConfigurer(@Value("${spexregister.crypto.secret-key}") final String secretKey) {
        final SocialSecurityNumberHasher hasher = new SocialSecurityNumberHasher(secretKey);
        final BeanConfigurer configurer = context ->
                context.define(SocialSecurityNumberHashBridge.class, BeanReference.ofInstance(new SocialSecurityNumberHashBridge(hasher)));

        return properties -> properties.put(EngineSpiSettings.BEAN_CONFIGURERS, List.of(BeanReference.ofInstance(configurer)));
    }
}
