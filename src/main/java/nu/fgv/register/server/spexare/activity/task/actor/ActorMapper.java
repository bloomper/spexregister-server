package nu.fgv.register.server.spexare.activity.task.actor;

import nu.fgv.register.server.settings.TypeMapper;
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
        uses = TypeMapper.class
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface ActorMapper {

    ActorMapper ACTOR_MAPPER = Mappers.getMapper(ActorMapper.class);

    ActorDto toDto(Actor model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vocal", ignore = true)
    @Mapping(target = "taskActivity", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    Actor toModel(ActorCreateDto dto);

    @Mapping(target = "vocal", ignore = true)
    @Mapping(target = "taskActivity", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    Actor toModel(ActorUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(ActorUpdateDto dto, @MappingTarget Actor model);
}
