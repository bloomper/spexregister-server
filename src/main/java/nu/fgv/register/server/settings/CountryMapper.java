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

package nu.fgv.register.server.settings;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface CountryMapper {

    CountryMapper COUNTRY_MAPPER = Mappers.getMapper(CountryMapper.class);

    @Mapping(target = "isoCode", source = "isoCode")
    @Mapping(target = "label", ignore = true)
    CountryDto toDto(String isoCode);

    CountryImpexDto toImpexDto(CountryDto country);

    List<CountryImpexDto> toImpexDtos(List<CountryDto> countries);

    @AfterMapping
    default void setLabel(final String isoCode, final @MappingTarget CountryDto.CountryDtoBuilder dto) {
        final Locale l = new Locale.Builder().setRegion(isoCode).build();

        dto.label(l.getDisplayCountry(LocaleContextHolder.getLocale()));
    }

}
