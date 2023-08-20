package nu.fgv.register.server.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository repository;

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

    private boolean doesUserExist(final Long id) {
        return repository.existsById(id);
    }

}
