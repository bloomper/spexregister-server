package nu.fgv.register.server.spex;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface SpexCategoryMapper {

    SpexCategoryDto toDto(SpexCategory model);

    List<SpexCategoryDto> toDtos(List<SpexCategory> models);

    @Mappings({
            @Mapping(target = "id", ignore = true)
    })
    SpexCategory toModel(SpexCategoryDto dto);

    List<SpexCategory> toModels(List<SpexCategoryDto> dtos);

    @AfterMapping
    default void setModelId(final SpexCategoryDto dto, final @MappingTarget SpexCategory model) {
        if (Objects.nonNull(dto.getId())) {
            model.setId(dto.getId());
        }
    }
}
