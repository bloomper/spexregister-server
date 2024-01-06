package nu.fgv.register.server.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.settings.TypeMapper.TYPE_MAPPER;
import static nu.fgv.register.server.settings.TypeSpecification.hasType;

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
                .toList();
    }

    public List<TypeDto> findByType(final TypeType type) {
        return repository
                .findAll(hasType(type))
                .stream()
                .map(TYPE_MAPPER::toDto)
                .toList();
    }

    public Optional<TypeDto> findById(final String id) {
        return repository
                .findById(id)
                .map(TYPE_MAPPER::toDto);
    }

    public boolean existsByIdAndType(final String id, final TypeType type) {
        return repository
                .findOne(TypeSpecification.hasId(id).and(TypeSpecification.hasType(type)))
                .map(t -> Boolean.TRUE)
                .orElse(Boolean.FALSE);

    }

}
