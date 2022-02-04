package nu.fgv.register.server.spex;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface SpexCategoryMapper {

    SpexDto toDto(Spex model);

    List<SpexDto> toDtos(List<Spex> models);

    Spex toModel(SpexDto dto);
}
