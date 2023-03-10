package nu.fgv.register.server.spexare.toggle;

import nu.fgv.register.server.settings.TypeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
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
public interface ToggleMapper {

    ToggleMapper TOGGLE_MAPPER = Mappers.getMapper(ToggleMapper.class);

    ToggleDto toDto(Toggle model);

}
