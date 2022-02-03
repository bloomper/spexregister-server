package nu.fgv.register.server.mapper;

import nu.fgv.register.server.dto.SpexDto;
import nu.fgv.register.server.model.Spex;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
        componentModel = "spring"
)
public interface SpexMapper {

    SpexDto toSpexDto(Spex model);

    List<SpexDto> toSpexDtos(List<Spex> models);

    Spex toSpex(SpexDto spexDto);
}
