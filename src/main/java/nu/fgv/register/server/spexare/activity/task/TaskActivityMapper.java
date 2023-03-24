package nu.fgv.register.server.spexare.activity.task;

import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
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
public interface TaskActivityMapper {

    TaskActivityMapper TASK_ACTIVITY_MAPPER = Mappers.getMapper(TaskActivityMapper.class);

    TaskActivityDto toDto(TaskActivity model);

}
