package nu.fgv.register.server.settings;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.context.i18n.LocaleContextHolder;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface TypeMapper {

    TypeMapper TYPE_MAPPER = Mappers.getMapper(TypeMapper.class);

    @Mapping(target = "label", ignore = true)
    TypeDto toDto(Type model);

    @AfterMapping
    default void setLabel(final Type model, final @MappingTarget TypeDto.TypeDtoBuilder dto) {
        dto.label(model.getLabels().get(LocaleContextHolder.getLocale().getLanguage()));
    }

}
