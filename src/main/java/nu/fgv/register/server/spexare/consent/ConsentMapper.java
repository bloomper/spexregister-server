package nu.fgv.register.server.spexare.consent;

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
public interface ConsentMapper {

    ConsentMapper CONSENT_MAPPER = Mappers.getMapper(ConsentMapper.class);

    ConsentDto toDto(Consent model);

}
