package nu.fgv.register.server.spex;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface SpexMapper {

    @Mapping(ignore = true, target = "details")
    SpexDto toDto(Spex model);

    List<SpexDto> toDtos(List<Spex> models);

    Spex toModel(SpexDto dto);
}
