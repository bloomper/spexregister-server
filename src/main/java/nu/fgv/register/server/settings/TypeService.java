package nu.fgv.register.server.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nu.fgv.register.server.settings.TypeMapper.TYPE_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
public class TypeService {

    private final TypeRepository repository;

    public List<TypeDto> findAll() {
        return repository
                .findAll()
                .stream()
                .map(TYPE_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public List<TypeDto> findByType(final TypeType type) {
        return repository
                .findByType(type)
                .stream()
                .map(TYPE_MAPPER::toDto)
                .collect(Collectors.toList());
    }


    public Optional<TypeDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(TYPE_MAPPER::toDto);
    }

}
