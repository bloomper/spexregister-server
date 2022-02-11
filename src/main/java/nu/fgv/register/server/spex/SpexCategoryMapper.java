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
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface SpexCategoryMapper {

    SpexCategoryMapper SPEX_CATEGORY_MAPPER = Mappers.getMapper(SpexCategoryMapper.class);

    @Mappings({
            @Mapping(target = "logo", ignore = true),
    })
    @BeanMapping(ignoreUnmappedSourceProperties = {"logo", "logoContentType"})
    SpexCategoryDto toDto(SpexCategory model);

    List<SpexCategoryDto> toDtos(List<SpexCategory> models);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "logo", ignore = true),
            @Mapping(target = "logoContentType", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdDate", ignore = true),
            @Mapping(target = "lastModifiedBy", ignore = true),
            @Mapping(target = "lastModifiedDate", ignore = true)
    })
    SpexCategory toModel(SpexCategoryRequestDto dto);

    @Mappings({
            @Mapping(target = "logo", ignore = true),
            @Mapping(target = "logoContentType", ignore = true),
    })
    SpexCategory toModel(SpexCategoryDto dto);

    @Mappings({
            @Mapping(target = "logo", ignore = true),
            @Mapping(target = "logoContentType", ignore = true),
    })
    void toPartialModel(SpexCategoryDto dto, @MappingTarget SpexCategory model);

    List<SpexCategory> toModels(List<SpexCategoryDto> dtos);

    @AfterMapping
    default void setLogo(final SpexCategory model, final @MappingTarget SpexCategoryDto.SpexCategoryDtoBuilder dto) {
        if (hasText(model.getLogoContentType())) {
            final Link logoLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SpexCategoryApi.class).downloadLogo(model.getId())).withRel("logo");
            dto.logo(logoLink.getHref());
        }
    }

}
