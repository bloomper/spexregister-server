package nu.fgv.register.server.spex;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface SpexCategoryMapper {

    SpexCategoryDto toDto(SpexCategory model);

    List<SpexCategoryDto> toDtos(List<SpexCategory> models);

    SpexCategory toModel(SpexCategoryDto dto);
}
