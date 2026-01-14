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

package nu.fgv.register.server.spexare.consent;

import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.settings.TypeMapper;
import nu.fgv.register.server.spexare.Spexare;
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
public interface ConsentMapper {

    ConsentMapper CONSENT_MAPPER = Mappers.getMapper(ConsentMapper.class);

    ConsentDto toDto(Consent model);

    @Mapping(target = "id", source = "consent.id")
    @Mapping(target = "spexareId", source = "spexare.id")
    @Mapping(target = "typeId", source = "type.id")
    @Mapping(target = "typeLabel", source = "type.label")
    @Mapping(target = "createdBy", source = "consent.createdBy")
    @Mapping(target = "createdAt", source = "consent.createdAt")
    @Mapping(target = "lastModifiedBy", source = "consent.lastModifiedBy")
    @Mapping(target = "lastModifiedAt", source = "consent.lastModifiedAt")
    ConsentImpexDto toImpexDto(Spexare spexare, Consent consent, TypeDto type);
}
