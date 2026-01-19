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

package nu.fgv.register.server.spexare.tagging;

import nu.fgv.register.server.settings.TypeMapper;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.tag.TagDto;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;


/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = TypeMapper.class
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface TaggingMapper {

    TaggingMapper TAGGING_MAPPER = Mappers.getMapper(TaggingMapper.class);

    @Mapping(target = "spexareId", source = "spexare.id")
    @Mapping(target = "tagId", source = "tag.id")
    @Mapping(target = "tagName", source = "tag.name")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.impex.model.ImpexAction.UPDATE)")
    @Mapping(target = "rowNumber", ignore = true)
    TaggingImpexDto toImpexDto(Spexare spexare, TagDto tag);
}
