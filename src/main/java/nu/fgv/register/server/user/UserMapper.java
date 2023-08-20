package nu.fgv.register.server.user;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

    UserMapper USER_MAPPER = Mappers.getMapper(UserMapper.class);

    UserDto toDto(User model);

    @Mappings({
            @Mapping(target = "state", expression = "java(User.State.PENDING)"),
            @Mapping(target = "federated", expression = "java(Boolean.FALSE)"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "authorities", ignore = true),
            @Mapping(target = "password", ignore = true),
            @Mapping(target = "spexare", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    User toModel(UserCreateDto dto);

    @Mappings({
            @Mapping(target = "state", ignore = true),
            @Mapping(target = "federated", ignore = true),
            @Mapping(target = "authorities", ignore = true),
            @Mapping(target = "password", ignore = true),
            @Mapping(target = "spexare", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    User toModel(UserUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(UserUpdateDto dto, @MappingTarget User model);

}
