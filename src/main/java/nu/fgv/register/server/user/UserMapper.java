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

package nu.fgv.register.server.user;

import nu.fgv.register.server.spexare.SpexareMapper;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.state.State;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.user.state.StateMapper;
import org.jspecify.annotations.Nullable;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {StateMapper.class, SpexareMapper.class}
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

    UserMapper USER_MAPPER = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", source = "model.id")
    @Mapping(target = "email", source = "representation.email")
    @Mapping(target = "temporaryPassword", source = "temporaryPassword")
    UserDto toDto(User model, UserRepresentation representation, @Nullable String temporaryPassword);

    @Mapping(target = "id", source = "model.id")
    @Mapping(target = "stateId", source = "state.id")
    @Mapping(target = "stateLabel", source = "state.label")
    @Mapping(target = "spexareId", source = "model.spexare.id")
    @Mapping(target = "spexareFirstName", source = "model.spexare.firstName")
    @Mapping(target = "spexareLastName", source = "model.spexare.lastName")
    @Mapping(target = "spexareNickName", source = "model.spexare.nickName")
    @Mapping(target = "email", source = "representation.email", defaultValue = "")
    @Mapping(target = "createdBy", source = "model.createdBy")
    @Mapping(target = "createdAt", source = "model.createdAt")
    @Mapping(target = "lastModifiedBy", source = "model.lastModifiedBy")
    @Mapping(target = "lastModifiedAt", source = "model.lastModifiedAt")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    UserImpexDto toImpexDto(User model, @Nullable UserRepresentation representation, StateDto state);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "spexare", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    User toModel(String externalId, State state);

    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "spexare", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    User toModel(UserUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(UserUpdateDto dto, @MappingTarget User model);

    default UserRepresentation toRepresentation(final UserCreateDto dto, final String temporaryPassword, final boolean enabled) {
        final UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setEmail(dto.email());
        userRepresentation.setEnabled(enabled);

        final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(temporaryPassword);
        credentialRepresentation.setTemporary(true);

        userRepresentation.setCredentials(List.of(credentialRepresentation));

        return userRepresentation;
    }

    default String extractLabel(final Map<String, String> labels, final Locale locale) {
        return labels.getOrDefault(locale.getLanguage(), labels.getOrDefault("sv", ""));
    }
}
