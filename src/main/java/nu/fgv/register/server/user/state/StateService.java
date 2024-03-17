package nu.fgv.register.server.user.state;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.user.state.StateMapper.STATE_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class StateService {

    private final StateRepository repository;

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<StateDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream().map(STATE_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<StateDto> findById(final String id) {
        return repository
                .findById(id)
                .map(STATE_MAPPER::toDto);
    }

}
