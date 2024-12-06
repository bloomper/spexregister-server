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
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class SettingsGraphqlApi {

    private final LanguageService languageService;
    private final CountryService countryService;
    private final TypeService typeService;

    @QueryMapping("languages")
    public List<LanguageDto> retrieveLanguages() {
        return languageService.findAll();
    }

    @QueryMapping("language")
    public LanguageDto retrieveLanguage(@Argument final String isoCode) {
        return languageService.findByIsoCode(isoCode);
    }

    @QueryMapping("countries")
    public List<CountryDto> retrieveCountries() {
        return countryService.findAll();
    }

    @QueryMapping("country")
    public CountryDto retrieveCountry(@Argument final String isoCode) {
        return countryService.findByIsoCode(isoCode);
    }

    @QueryMapping("types")
    public List<TypeDto> retrieveTypes() {
        return typeService.findAll();
    }

    @QueryMapping("typesOfType")
    public List<TypeDto> retrieveTypes(@Argument final TypeType type) {
        return typeService.findByType(type);
    }

    @QueryMapping("type")
    public TypeDto retrieveType(@Argument final String id) {
        return typeService.findById(id);
    }
}
