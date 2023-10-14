package nu.fgv.register.server.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.user.authority.Authority;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.authority.AuthorityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;
import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final AuthorityRepository authorityRepository;

    public Page<UserDto> find(final Pageable pageable) {
        return repository
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

    public Set<AuthorityDto> findAuthoritiesByUser(final Long userId) {
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

    private boolean doesUserExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doUserAndAuthorityExist(final Long userId, final String authorityId) {
        return doesUserExist(userId) && authorityRepository.existsById(authorityId);
    }

    private boolean doUserAndAuthoritiesExist(final Long userId, final List<String> authorityIds) {
        return doesUserExist(userId) && authorityIds.stream().allMatch(authorityRepository::existsById);
    }

}
