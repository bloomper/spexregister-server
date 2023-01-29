package nu.fgv.register.server.spexare;

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
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface SpexareMapper {

    SpexareMapper SPEXARE_MAPPER = Mappers.getMapper(SpexareMapper.class);

    SpexareDto toDto(Spexare model);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Spexare toModel(SpexareCreateDto dto);

    @Mappings({
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Spexare toModel(SpexareUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(SpexareUpdateDto dto, @MappingTarget Spexare model);

}
