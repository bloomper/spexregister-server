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

package nu.fgv.register.server.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.config.SpexregisterConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class LanguageService {

    private final SpexregisterConfig spexregisterConfig;

    private final MessageSource messageSource;

    public List<LanguageDto> findAll() {
        return spexregisterConfig.getLanguages()
                .stream()
                .map(this::mapDto)
                .sorted(Comparator.comparing(LanguageDto::getLabel))
                .toList();
    }

    public Optional<LanguageDto> findByIsoCode(final String isoCode) {
        return spexregisterConfig.getLanguages()
                .stream()
                .filter(l -> l.equals(isoCode))
                .map(this::mapDto)
                .findFirst();

    }

    private LanguageDto mapDto(final String isoCode) {
        return LanguageDto.builder()
                .isoCode(isoCode)
                .label(messageSource.getMessage(String.format("language.%s.label", isoCode), new Object[]{}, LocaleContextHolder.getLocale())).build();
    }

}
