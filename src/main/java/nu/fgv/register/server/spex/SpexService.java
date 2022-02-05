package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexService {

    private final SpexRepository repository;
    private final SpexMapper mapper;

    public Page<SpexDto> find(final Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public Optional<SpexDto> findById(final Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public SpexDto save(final SpexDto dto) {
        return mapper.toDto(repository.save(mapper.toModel(dto)));
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public List<SpexDto> findAllRevivals(final Long id) {
        return repository.findAllRevivals(id).stream().map(mapper::toDto).collect(Collectors.toList());
    }
}
