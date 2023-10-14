package nu.fgv.register.server.user.state;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nu.fgv.register.server.user.state.StateMapper.STATE_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class StateService {

    private final StateRepository repository;

    public List<StateDto> findAll() {
        return repository
                .findAll()
                .stream().map(STATE_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public Optional<StateDto> findById(final String id) {
        return repository
                .findById(id)
                .map(STATE_MAPPER::toDto);
    }

}
