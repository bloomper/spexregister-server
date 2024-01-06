package nu.fgv.register.server.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

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

    public Optional<CountryDto> findByIsoCode(final String isoCode) {
        return Stream.of(Locale.getISOCountries())
                .filter(c -> c.equalsIgnoreCase(isoCode))
                .map(this::mapDto)
                .findFirst();
    }

    private CountryDto mapDto(final String isoCode) {
        final Locale l = new Locale.Builder().setRegion(isoCode).build();

        return CountryDto.builder()
                .isoCode(isoCode)
                .label(l.getDisplayCountry(LocaleContextHolder.getLocale())).build();
    }

}
