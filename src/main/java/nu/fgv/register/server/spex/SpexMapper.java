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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.List;
import java.util.Objects;

import static org.springframework.util.StringUtils.hasText;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = SpexCategoryMapper.class
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface SpexMapper {

    @Mappings({
            @Mapping(target = "poster", ignore = true),
            @Mapping(target = "title", source = "details.title")
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