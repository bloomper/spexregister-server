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
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsApi {

    private final LanguageService languageService;
    private final CountryService countryService;
    private final TypeService typeService;

    @GetMapping(value = "/languages", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<LanguageDto>>> retrieveLanguages() {
        final List<EntityModel<LanguageDto>> languages = languageService.findAll().stream()
                .map(language -> EntityModel.of(language,
                        linkTo(methodOn(SettingsApi.class).retrieveLanguage(language.getIsoCode())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveLanguages()).withRel("languages")))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(languages,
                        linkTo(methodOn(SettingsApi.class).retrieveLanguages()).withSelfRel()));
    }

    @GetMapping(value = "/languages/{isoCode}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<LanguageDto>> retrieveLanguage(@PathVariable final String isoCode) {
        return languageService.findByIsoCode(isoCode)
                .map(language -> EntityModel.of(language,
                        linkTo(methodOn(SettingsApi.class).retrieveLanguage(language.getIsoCode())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveLanguages()).withRel("languages")))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/countries", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<CountryDto>>> retrieveCountries() {
        final List<EntityModel<CountryDto>> countries = countryService.findAll().stream()
                .map(country -> EntityModel.of(country,
                        linkTo(methodOn(SettingsApi.class).retrieveCountry(country.getIsoCode())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveCountries()).withRel("countries")))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(countries,
                        linkTo(methodOn(SettingsApi.class).retrieveCountries()).withSelfRel()));
    }

    @GetMapping(value = "/countries/{isoCode}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<CountryDto>> retrieveCountry(@PathVariable final String isoCode) {
        return countryService.findByIsoCode(isoCode)
                .map(country -> EntityModel.of(country,
                        linkTo(methodOn(SettingsApi.class).retrieveCountry(country.getIsoCode())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveCountries()).withRel("countries")))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/types", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<TypeDto>>> retrieveTypes() {
        final List<EntityModel<TypeDto>> types = typeService.findAll().stream()
                .map(type -> EntityModel.of(type,
                        linkTo(methodOn(SettingsApi.class).retrieveType(type.getType(), type.getId())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveTypes()).withRel("types")))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(types,
                        linkTo(methodOn(SettingsApi.class).retrieveTypes()).withSelfRel()));
    }

    @GetMapping(value = "/types/{type}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<TypeDto>>> retrieveTypes(@PathVariable final TypeType type) {
        final List<EntityModel<TypeDto>> types = typeService.findByType(type).stream()
                .map(type0 -> EntityModel.of(type0,
                        linkTo(methodOn(SettingsApi.class).retrieveType(type, type0.getId())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveTypes(type)).withRel("types")))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(types,
                        linkTo(methodOn(SettingsApi.class).retrieveTypes(type)).withSelfRel()));
    }

    @GetMapping(value = "/types/{type}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TypeDto>> retrieveType(@PathVariable final TypeType type, @PathVariable final String id) {
        return typeService.findById(id)
                .map(type0 -> EntityModel.of(type0,
                        linkTo(methodOn(SettingsApi.class).retrieveType(type, type0.getId())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveTypes()).withRel("types")))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
