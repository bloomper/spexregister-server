package nu.fgv.register.server.mapper;

import nu.fgv.register.server.dto.SpexCategoryDto;
import nu.fgv.register.server.model.SpexCategory;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
        componentModel = "spring"
)
public interface SpexCategoryMapper {

    SpexCategoryDto toSpexCategoryDto(SpexCategory model);

    List<SpexCategoryDto> toSpexCategoryDtos(List<SpexCategory> models);

    SpexCategory toSpexCategory(SpexCategoryDto dto);
}
