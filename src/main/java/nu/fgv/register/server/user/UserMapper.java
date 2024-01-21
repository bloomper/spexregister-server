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
    UserDto toDto(User model, UserRepresentation representation, String temporaryPassword);

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

    default UserRepresentation toRepresentation(UserCreateDto dto, String temporaryPassword) {
        final UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setEmail(dto.getEmail());
        userRepresentation.setEnabled(true);

        final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(temporaryPassword);
        credentialRepresentation.setTemporary(true);

        return userRepresentation;
    }

}
