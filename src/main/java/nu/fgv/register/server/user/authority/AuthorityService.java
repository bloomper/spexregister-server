package nu.fgv.register.server.user.authority;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AuthorityService {

    private final AuthorityRepository repository;

    public List<AuthorityDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream().map(AUTHORITY_MAPPER::toDto)
                .toList();
    }

    public Optional<AuthorityDto> findById(final String id) {
        return repository
                .findById(id)
                .map(AUTHORITY_MAPPER::toDto);
    }

}
