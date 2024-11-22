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

import nu.fgv.register.server.user.state.StateMapper;
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
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {StateMapper.class}
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "spexare", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    User toModel(String externalId);

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

    default UserRepresentation toRepresentation(final UserCreateDto dto, final String temporaryPassword) {
        final UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setEmail(dto.getEmail());
        userRepresentation.setEnabled(true);

        final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(temporaryPassword);
        credentialRepresentation.setTemporary(true);

        userRepresentation.setCredentials(List.of(credentialRepresentation));

        return userRepresentation;
    }

}
