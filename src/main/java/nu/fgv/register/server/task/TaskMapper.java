package nu.fgv.register.server.task;

import nu.fgv.register.server.spex.SpexCategoryMapper;
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
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = SpexCategoryMapper.class
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface TaskMapper {

    TaskMapper TASK_MAPPER = Mappers.getMapper(TaskMapper.class);

    TaskDto toDto(Task model);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Task toModel(TaskCreateDto dto);

    @Mappings({
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Task toModel(TaskUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(TaskUpdateDto dto, @MappingTarget Task model);

}
