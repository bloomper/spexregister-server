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
