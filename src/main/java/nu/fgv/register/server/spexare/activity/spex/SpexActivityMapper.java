package nu.fgv.register.server.spexare.activity.spex;

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
public interface SpexActivityMapper {

    SpexActivityMapper SPEX_ACTIVITY_MAPPER = Mappers.getMapper(SpexActivityMapper.class);

    SpexActivityDto toDto(SpexActivity model);

}
