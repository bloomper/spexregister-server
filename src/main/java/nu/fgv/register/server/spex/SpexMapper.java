package nu.fgv.register.server.spex;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
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
public interface SpexMapper {

    SpexMapper SPEX_MAPPER = Mappers.getMapper(SpexMapper.class);

    @Mappings({
            @Mapping(target = "title", source = "details.title"),
    })
    @BeanMapping(ignoreUnmappedSourceProperties = {"details"})
    SpexDto toDto(Spex model);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "details.title", source = "title"),
            @Mapping(target = "parent", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Spex toModel(SpexCreateDto dto);

    @Mappings({
            @Mapping(target = "details.title", source = "title"),
            @Mapping(target = "details.category", ignore = true),
            @Mapping(target = "details.createdBy", ignore = true),
            @Mapping(target = "details.createdAt", ignore = true),
            @Mapping(target = "details.lastModifiedBy", ignore = true),
            @Mapping(target = "details.lastModifiedAt", ignore = true),
            @Mapping(target = "parent", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Spex toModel(SpexUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(SpexUpdateDto dto, @MappingTarget Spex model);

    @AfterMapping
    default void setRevival(final Spex model, final @MappingTarget SpexDto.SpexDtoBuilder dto) {
        dto.revival(model.isRevival());
    }

}
