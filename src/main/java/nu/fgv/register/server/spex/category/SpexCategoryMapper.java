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

package nu.fgv.register.server.spex.category;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {Optional.class, WebMvcLinkBuilder.class}
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface SpexCategoryMapper {

    SpexCategoryMapper SPEX_CATEGORY_MAPPER = Mappers.getMapper(SpexCategoryMapper.class);

    @BeanMapping(ignoreUnmappedSourceProperties = {"logo", "logoContentType"})
    @Mapping(target = "logoUrl", expression = "java(Optional.ofNullable(model.getLogo()).map(logo -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexCategoryApi.class).downloadLogo(model.getId())).toUri().toString()).orElse(null))")
    SpexCategoryDto toDto(SpexCategory model);

    @BeanMapping(ignoreUnmappedSourceProperties = {"logo", "logoContentType"})
    @Mapping(target = "logoUrl", expression = "java(Optional.ofNullable(model.getLogo()).map(logo -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexCategoryApi.class).downloadLogo(model.getId())).toUri().toString()).orElse(null))")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    @Mapping(target = "rowNumber", ignore = true)
    SpexCategoryImpexDto toImpexDto(SpexCategory model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    SpexCategory toModel(SpexCategoryCreateDto dto);

    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    SpexCategory toModel(SpexCategoryUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(SpexCategoryUpdateDto dto, @MappingTarget SpexCategory model);

    SpexCategoryCreateDto toCreateDto(SpexCategoryImpexDto dto);

    SpexCategoryUpdateDto toUpdateDto(SpexCategoryImpexDto dto);
}
