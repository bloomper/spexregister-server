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
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CountryService {

    public List<CountryDto> findAll() {
        return Stream.of(Locale.getISOCountries())
                .map(this::mapDto)
                .sorted(Comparator.comparing(CountryDto::getLabel))
                .toList();
    }

    public CountryDto findByIsoCode(final String isoCode) {
        return Stream.of(Locale.getISOCountries())
                .filter(c -> c.equalsIgnoreCase(isoCode))
                .map(this::mapDto)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Country", isoCode));
    }

    private CountryDto mapDto(final String isoCode) {
        final Locale l = new Locale.Builder().setRegion(isoCode).build();

        return CountryDto.builder()
                .isoCode(isoCode)
                .label(l.getDisplayCountry(LocaleContextHolder.getLocale())).build();
    }

}
