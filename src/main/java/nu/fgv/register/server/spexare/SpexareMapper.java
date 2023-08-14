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

import java.util.List;

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

    List<SpexareDto> toDtos(List<Spexare> models);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "socialSecurityNumber", ignore = true),
            @Mapping(target = "graduation", ignore = true),
            @Mapping(target = "comment", ignore = true),
            @Mapping(target = "image", ignore = true),
            @Mapping(target = "imageContentType", ignore = true),
            @Mapping(target = "partner", ignore = true),
            @Mapping(target = "user", ignore = true),
            @Mapping(target = "activities", ignore = true),
            @Mapping(target = "tags", ignore = true),
            @Mapping(target = "addresses", ignore = true),
            @Mapping(target = "memberships", ignore = true),
            @Mapping(target = "consents", ignore = true),
            @Mapping(target = "toggles", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Spexare toModel(SpexareCreateDto dto);

    @Mappings({
            @Mapping(target = "image", ignore = true),
            @Mapping(target = "imageContentType", ignore = true),
            @Mapping(target = "partner", ignore = true),
            @Mapping(target = "user", ignore = true),
            @Mapping(target = "activities", ignore = true),
            @Mapping(target = "tags", ignore = true),
            @Mapping(target = "addresses", ignore = true),
            @Mapping(target = "memberships", ignore = true),
            @Mapping(target = "consents", ignore = true),
            @Mapping(target = "toggles", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedAt", ignore = true)
    })
    Spexare toModel(SpexareUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(SpexareUpdateDto dto, @MappingTarget Spexare model);

}
