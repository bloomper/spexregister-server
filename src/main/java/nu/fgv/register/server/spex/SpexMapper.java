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

package nu.fgv.register.server.spex;

import nu.fgv.register.server.spex.category.SpexCategoryMapper;
import org.mapstruct.AfterMapping;
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
        uses = SpexCategoryMapper.class,
        imports = {Optional.class, WebMvcLinkBuilder.class}
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface SpexMapper {

    SpexMapper SPEX_MAPPER = Mappers.getMapper(SpexMapper.class);

    @Mapping(target = "title", source = "details.title")
    @Mapping(target = "posterUrl", expression = "java(Optional.ofNullable(model.getDetails().getPoster()).map(poster -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexApi.class).downloadPoster(model.getId())).toUri().toString()).orElse(null))")
    @BeanMapping(ignoreUnmappedSourceProperties = {"details"})
    SpexDto toDto(Spex model);

    @Mapping(target = "title", source = "details.title")
    @Mapping(target = "posterUrl", expression = "java(Optional.ofNullable(model.getDetails().getPoster()).map(poster -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexApi.class).downloadPoster(model.getId())).toUri().toString()).orElse(null))")
    @Mapping(target = "categoryId", source = "details.category.id")
    @Mapping(target = "categoryName", source = "details.category.name")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    @BeanMapping(ignoreUnmappedSourceProperties = {"details"})
    @Mapping(target = "rowNumber", ignore = true)
    SpexImpexDto toImpexDto(Spex model);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentYear", source = "parent.year")
    @Mapping(target = "parentTitle", source = "details.title")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    @BeanMapping(ignoreUnmappedSourceProperties = {"details"})
    @Mapping(target = "rowNumber", ignore = true)
    SpexRevivalImpexDto toRevivalImpexDto(Spex model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "details.title", source = "title")
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    Spex toModel(SpexCreateDto dto);

    @Mapping(target = "details.title", source = "title")
    @Mapping(target = "details.category", ignore = true)
    @Mapping(target = "details.createdBy", ignore = true)
    @Mapping(target = "details.createdAt", ignore = true)
    @Mapping(target = "details.lastModifiedBy", ignore = true)
    @Mapping(target = "details.lastModifiedAt", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    Spex toModel(SpexUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(SpexUpdateDto dto, @MappingTarget Spex model);

    SpexCreateDto toCreateDto(SpexImpexDto dto);

    SpexUpdateDto toUpdateDto(SpexImpexDto dto);

    @AfterMapping
    default void setRevival(final Spex model, final @MappingTarget SpexDto.SpexDtoBuilder dto) {
        dto.revival(model.isRevival());
    }

}
