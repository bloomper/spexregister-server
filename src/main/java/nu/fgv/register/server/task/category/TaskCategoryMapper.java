package nu.fgv.register.server.task.category;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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
public interface TaskCategoryMapper {

    TaskCategoryMapper TASK_CATEGORY_MAPPER = Mappers.getMapper(TaskCategoryMapper.class);

    TaskCategoryDto toDto(TaskCategory model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    TaskCategory toModel(TaskCategoryCreateDto dto);

    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    TaskCategory toModel(TaskCategoryUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(TaskCategoryUpdateDto dto, @MappingTarget TaskCategory model);

}
