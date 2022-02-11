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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

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
            @Mapping(target = "poster", ignore = true),
            @Mapping(target = "title", source = "details.title"),
            @Mapping(target = "category", source = "details.category")
    })
    @BeanMapping(ignoreUnmappedSourceProperties = {"details"})
    SpexDto toDto(Spex model);

    List<SpexDto> toDtos(List<Spex> models);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "details", ignore = true),
            @Mapping(target = "parent", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdDate", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedDate", ignore = true)
    })
    Spex toModel(SpexRequestDto dto);

    @Mappings({
            @Mapping(target = "details", ignore = true)
    })
    Spex toModel(SpexDto dto);

    @Mappings({
            @Mapping(target = "details", ignore = true),
    })
    void toPartialModel(SpexDto dto, @MappingTarget Spex model);

    List<Spex> toModels(List<SpexDto> dtos);

    @AfterMapping
    default void setRevival(final Spex model, final @MappingTarget SpexDto.SpexDtoBuilder dto) {
        dto.revival(model.isRevival());
    }

    @AfterMapping
    default void setPoster(final Spex model, final @MappingTarget SpexDto.SpexDtoBuilder dto) {
        if (hasText(model.getDetails().getPosterContentType())) {
            final Link posterLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexApi.class).downloadPoster(model.getId())).withRel("poster");
            dto.poster(posterLink.getHref());
        }
    }
}
