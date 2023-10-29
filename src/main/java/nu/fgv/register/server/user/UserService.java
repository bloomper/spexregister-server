package nu.fgv.register.server.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.authority.Authority;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.authority.AuthorityRepository;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;
import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;
import static nu.fgv.register.server.user.state.StateMapper.STATE_MAPPER;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final AuthorityRepository authorityRepository;
    private final StateRepository stateRepository;
    private final SpexareRepository spexareRepository;

    public Page<UserDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<User>builder().build(FilterParser.parse(filter), UserSpecification::new), pageable)
                        .map(USER_MAPPER::toDto) :
                repository
                        .findAll(pageable)
                        .map(USER_MAPPER::toDto);
    }

    public Optional<UserDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(USER_MAPPER::toDto);
    }

    public UserDto create(final UserCreateDto dto) {
        final User model = USER_MAPPER.toModel(dto);
        return USER_MAPPER.toDto(repository.save(model));
    }

    public Optional<UserDto> update(final UserUpdateDto dto) {
        return partialUpdate(dto);
    }

    public Optional<UserDto> partialUpdate(final UserUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(user -> {
                    USER_MAPPER.toPartialModel(dto, user);
                    return user;
                })
                .map(repository::save)
                .map(USER_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public Set<AuthorityDto> getAuthoritiesByUser(final Long userId) {
        if (doesUserExist(userId)) {
            return repository
                    .findById(userId)
                    .map(User::getAuthorities)
                    .map(AUTHORITY_MAPPER::toDtos)
                    .orElse(Collections.emptySet());
        } else {
            throw new ResourceNotFoundException(String.format("User %s does not exist", userId));
        }
    }

    public boolean addAuthorities(final Long userId, final List<String> ids) {
        if (doUserAndAuthoritiesExist(userId, ids)) {
            final List<Authority> authorities = authorityRepository.findAllById(ids);

            return repository
                    .findById(userId)
                    .map(user -> {
                        user.getAuthorities().addAll(authorities);
                        repository.save(user);
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or at least one authority in %s do not exist", userId, String.join(",", ids)));
        }
    }

    public boolean addAuthority(final Long userId, final String id) {
        if (doUserAndAuthorityExist(userId, id)) {
            return repository
                    .findById(userId)
                    .map(user -> authorityRepository
                            .findById(id)
                            .map(authority -> {
                                user.getAuthorities().add(authority);
                                repository.save(user);
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or authority %s do not exist", userId, id));
        }
    }

    public boolean removeAuthority(final Long userId, final String id) {
        if (doUserAndAuthorityExist(userId, id)) {
            return repository
                    .findById(userId)
                    .filter(user -> user.getAuthorities() != null)
                    .map(user -> authorityRepository
                            .findById(id)
                            .map(authority -> {
                                user.getAuthorities().remove(authority);
                                repository.save(user);
                                return true;
                            })
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or authority %s do not exist", userId, id));
        }
    }

    public boolean removeAuthorities(final Long userId, final List<String> ids) {
        if (doUserAndAuthoritiesExist(userId, ids)) {
            final List<Authority> authorities = authorityRepository.findAllById(ids);

            return repository
                    .findById(userId)
                    .map(user -> {
                        authorities.forEach(user.getAuthorities()::remove);
                        repository.save(user);
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or at least one authority in %s do not exist", userId, String.join(",", ids)));
        }
    }

    public StateDto getStateByUser(final Long id) {
        return repository
                .findById(id)
                .map(User::getState)
                .map(STATE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User %s does not exist", id)));
    }

    public boolean setState(final Long userId, final String id) {
        if (doUserAndStateExist(userId, id)) {
            return repository
                    .findById(userId)
                    .map(user -> stateRepository
                            .findById(id)
                            .map(state -> {
                                user.setState(state);
                                repository.save(user);
                                return true;
                            })
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or state %s does not exist", userId, id));
        }
    }

    public Optional<SpexareDto> findSpexareByUser(final Long userId) {
        if (doesUserExist(userId)) {
            return repository
                    .findById(userId)
                    .map(User::getSpexare)
                    .map(SPEXARE_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("User %s does not exist", userId));
        }
    }

    public boolean addSpexare(final Long userId, final Long id) {
        if (doUserAndSpexareExist(userId, id)) {
            return repository
                    .findById(userId)
                    .map(user -> spexareRepository
                            .findById(id)
                            .map(spexare -> {
                                user.setSpexare(spexare);
                                repository.save(user);
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s and/or spexare %s do not exist", userId, id));
        }
    }

    public boolean removeSpexare(final Long userId) {
        if (doesUserExist(userId)) {
            return repository
                    .findById(userId)
                    .filter(user -> user.getSpexare() != null)
                    .map(user -> {
                        user.setSpexare(null);
                        repository.save(user);
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("User %s does not exist", userId));
        }
    }


    private boolean doesUserExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doUserAndAuthorityExist(final Long userId, final String authorityId) {
        return doesUserExist(userId) && authorityRepository.existsById(authorityId);
    }

    private boolean doUserAndAuthoritiesExist(final Long userId, final List<String> authorityIds) {
        return doesUserExist(userId) && authorityIds.stream().allMatch(authorityRepository::existsById);
    }

    private boolean doUserAndStateExist(final Long userId, final String stateId) {
        return doesUserExist(userId) && stateRepository.existsById(stateId);
    }

    private boolean doUserAndSpexareExist(final Long userId, final Long spexareId) {
        return doesUserExist(userId) && spexareRepository.existsById(spexareId);
    }
}
