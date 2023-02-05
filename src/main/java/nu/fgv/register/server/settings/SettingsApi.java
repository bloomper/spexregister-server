package nu.fgv.register.server.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
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

    @GetMapping("/language")
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

    @GetMapping("/language/{isoCode}")
    public ResponseEntity<EntityModel<LanguageDto>> retrieveLanguage(@PathVariable final String isoCode) {
        return languageService.findByIsoCode(isoCode)
                .map(language -> EntityModel.of(language,
                        linkTo(methodOn(SettingsApi.class).retrieveLanguage(language.getIsoCode())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveLanguages()).withRel("languages")))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/country")
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

    @GetMapping("/country/{isoCode}")
    public ResponseEntity<EntityModel<CountryDto>> retrieveCountry(@PathVariable final String isoCode) {
        return countryService.findByIsoCode(isoCode)
                .map(country -> EntityModel.of(country,
                        linkTo(methodOn(SettingsApi.class).retrieveCountry(country.getIsoCode())).withSelfRel(),
                        linkTo(methodOn(SettingsApi.class).retrieveCountries()).withRel("countries")))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
