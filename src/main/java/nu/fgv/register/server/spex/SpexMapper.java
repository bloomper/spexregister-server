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

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "details.title", source = "title"),
            @Mapping(target = "parent", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdDate", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedDate", ignore = true)
    })
    Spex toModel(SpexCreateDto dto);

    @Mappings({
            @Mapping(target = "details.title", source = "title"),
            @Mapping(target = "details.category", ignore = true),
            @Mapping(target = "details.createdBy", ignore = true),
            @Mapping(target = "details.createdDate", ignore = true),
            @Mapping(target = "details.lastModifiedBy", ignore = true),
            @Mapping(target = "details.lastModifiedDate", ignore = true),
            @Mapping(target = "parent", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdDate", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedDate", ignore = true)
    })
    Spex toModel(SpexUpdateDto dto);

    @Mappings({
            @Mapping(target = "details", ignore = true),
            @Mapping(target = "parent", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdDate", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedDate", ignore = true)
    })
    void toPartialModel(SpexUpdateDto dto, @MappingTarget Spex model);

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
