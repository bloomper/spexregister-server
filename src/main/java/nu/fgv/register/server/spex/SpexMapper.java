package nu.fgv.register.server.spex;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Objects;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface SpexMapper {

    SpexMapper SPEX_MAPPER = Mappers.getMapper( SpexMapper.class );

    @Mappings({
            @Mapping(target = "details", ignore = true),
    })
    @BeanMapping(ignoreUnmappedSourceProperties = {"details"})
    SpexDto toDto(Spex model);

    List<SpexDto> toDtos(List<Spex> models);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "details", ignore = true)
    })
    Spex toModel(SpexDto dto);

    List<Spex> toModels(List<SpexDto> dtos);

    @AfterMapping
    default void setModelId(final SpexDto dto, final @MappingTarget Spex model) {
        if (Objects.nonNull(dto.getId())) {
            model.setId(dto.getId());
        }
    }
}
