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

package nu.fgv.register.server.user.authority;

import nu.fgv.register.server.user.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Set;

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
public interface AuthorityMapper {

    AuthorityMapper AUTHORITY_MAPPER = Mappers.getMapper(AuthorityMapper.class);

    @Mapping(target = "label", ignore = true)
    AuthorityDto toDto(Authority model);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "id", source = "authority.id")
    @Mapping(target = "label", source = "authority.label")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    AuthorityImpexDto toImpexDto(User user, AuthorityDto authority);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "label", source = "label")
    @Mapping(target = "action", ignore = true)
    AuthorityImpexDto toImpexDto(AuthorityDto authority);

    Set<AuthorityDto> toDtos(Set<Authority> models);

    @AfterMapping
    default void setLabel(final Authority model, final @MappingTarget AuthorityDto.AuthorityDtoBuilder dto) {
        dto.label(model.getLabels().get(LocaleContextHolder.getLocale().getLanguage()));
    }

}
