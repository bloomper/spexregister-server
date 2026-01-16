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

package nu.fgv.register.server.spexare;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.List;
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
public interface SpexareMapper {

    SpexareMapper SPEXARE_MAPPER = Mappers.getMapper(SpexareMapper.class);

    @Mapping(target = "imageUrl", expression = "java(Optional.ofNullable(model.getImage()).map(poster -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexareApi.class).downloadImage(model.getId())).toUri().toString()).orElse(null))")
    SpexareDto toDto(Spexare model);

    @Mapping(target = "imageUrl", expression = "java(Optional.ofNullable(model.getImage()).map(poster -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexareApi.class).downloadImage(model.getId())).toUri().toString()).orElse(null))")
    @Mapping(target = "partnerId", source = "partner.id")
    @Mapping(target = "partnerFirstName", source = "partner.firstName")
    @Mapping(target = "partnerLastName", source = "partner.lastName")
    @Mapping(target = "partnerNickName", source = "partner.nickName")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    SpexareImpexDto toImpexDto(Spexare model);

    List<SpexareDto> toDtos(List<Spexare> models);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "socialSecurityNumber", ignore = true)
    @Mapping(target = "graduation", ignore = true)
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    @Mapping(target = "partner", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "consents", ignore = true)
    @Mapping(target = "toggles", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    Spexare toModel(SpexareCreateDto dto);

    @Mapping(target = "image", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    @Mapping(target = "partner", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "consents", ignore = true)
    @Mapping(target = "toggles", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    Spexare toModel(SpexareUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(SpexareUpdateDto dto, @MappingTarget Spexare model);

}
