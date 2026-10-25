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
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static nu.fgv.register.server.settings.CountryMapper.COUNTRY_MAPPER;

/**
 * The countries of ISO 3166-1, labelled in the caller's language.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CountryService {

    /**
     * @return every country, by label
     */
    public List<CountryDto> findAll() {
        return Stream.of(Locale.getISOCountries())
                .map(COUNTRY_MAPPER::toDto)
                .sorted(Comparator.comparing(CountryDto::getLabel))
                .toList();
    }

    /**
     * @param isoCode an ISO 3166-1 alpha-2 code, in any case
     * @return the country
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is none
     */
    public CountryDto findByIsoCode(final String isoCode) {
        return Stream.of(Locale.getISOCountries())
                .filter(c -> c.equalsIgnoreCase(isoCode))
                .map(COUNTRY_MAPPER::toDto)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Country", isoCode));
    }

    /**
     * @param isoCode an ISO 3166-1 alpha-2 code, in any case
     * @return whether there is such a country
     */
    public boolean exists(final String isoCode) {
        return Stream.of(Locale.getISOCountries())
                .anyMatch(c -> c.equalsIgnoreCase(isoCode));
    }

}
