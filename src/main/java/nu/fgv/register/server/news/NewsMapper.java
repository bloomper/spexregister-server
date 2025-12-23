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

package nu.fgv.register.server.news;

import org.jspecify.annotations.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

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
public interface NewsMapper {

    NewsMapper NEWS_MAPPER = Mappers.getMapper(NewsMapper.class);

    NewsDto toDto(News model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    News toModel(NewsCreateDto dto);

    @Mapping(target = "published", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    News toModel(NewsUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(NewsUpdateDto dto, @MappingTarget News model);

    @AfterMapping
    default void setPublished(final NewsCreateDto dto, final @MappingTarget News model) {
        model.setPublished(isPublished(dto.visibleFrom(), dto.visibleTo()));
    }

    @AfterMapping
    default void setPublished(final NewsUpdateDto dto, final @MappingTarget News model) {
        model.setPublished(isPublished(dto.visibleFrom(), dto.visibleTo()));
    }

    default boolean isPublished(@Nullable final LocalDate visibleFrom, @Nullable final LocalDate visibleTo) {
        final LocalDate today = LocalDate.now();

        return (visibleFrom != null && (visibleFrom.isEqual(today) || visibleFrom.isBefore(today))) &&
                (visibleTo == null || visibleTo.isEqual(today) || visibleTo.isAfter(today));
    }
}
