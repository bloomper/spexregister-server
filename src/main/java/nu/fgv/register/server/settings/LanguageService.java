package nu.fgv.register.server.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class LanguageService {

    @Value("${spexregister.languages}}")
    private List<String> languages;

    private final MessageSource messageSource;

    public List<LanguageDto> findAll() {
        return languages
                .stream()
                .map(this::mapDto)
                .sorted(Comparator.comparing(LanguageDto::getLabel))
                .collect(Collectors.toList());
    }

    public Optional<LanguageDto> findByIsoCode(final String isoCode) {
        return languages
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
