package nu.fgv.register.server.user.state;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Set;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface StateMapper {

    StateMapper STATE_MAPPER = Mappers.getMapper(StateMapper.class);

    @Mapping(target = "label", ignore = true)
    StateDto toDto(State model);

    Set<StateDto> toDtos(Set<State> models);

    @AfterMapping
    default void setLabel(final State model, final @MappingTarget StateDto.StateDtoBuilder dto) {
        dto.label(model.getLabels().get(LocaleContextHolder.getLocale().getLanguage()));
    }
}
