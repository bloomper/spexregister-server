package nu.fgv.register.server.spexare.toggle;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.SpexareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spexare.toggle.ToggleMapper.TOGGLE_MAPPER;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasId;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.toggle.ToggleSpecification.hasType;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ToggleService {

    private final ToggleRepository repository;

    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;
    private final TypeService typeService;

    public Page<ToggleDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> repository
                            .findAll(hasSpexare(spexare), pageable)
                            .map(TOGGLE_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<ToggleDto> findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(id)
                    .filter(toggle -> toggle.getSpexare().getId().equals(spexareId))
                    .map(TOGGLE_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<ToggleDto> create(final Long spexareId, final String typeId, final Boolean value) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> !repository.exists(hasSpexare(spexare).and(hasType(type))))
                            .map(spexare -> {
                                final Toggle toggle = new Toggle();
                                toggle.setSpexare(spexare);
                                toggle.setType(type);
                                toggle.setValue(value);
                                return repository.save(toggle);
                            })
                            .map(TOGGLE_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or type %s do not exist", spexareId, typeId));
        }
    }

    public Optional<ToggleDto> update(final Long spexareId, final String typeId, final Long id, final Boolean value) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesToggleExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(toggle -> toggle.getSpexare().getId().equals(spexareId))
                            .map(toggle -> {
                                toggle.setValue(value);
                                return repository.save(toggle);
                            })
                            .map(TOGGLE_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, type %s and/or toggle %s do not exist", spexareId, typeId, id));
        }
    }

    public boolean deleteById(final Long spexareId, final String typeId, final Long id) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesToggleExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .map(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(toggle -> toggle.getSpexare().getId().equals(spexareId))
                            .map(toggle -> {
                                repository.deleteById(toggle.getId());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, type %s and/or toggle %s do not exist", spexareId, typeId, id));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesToggleExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeService.existsByIdAndType(typeId, TypeType.TOGGLE);
    }

}
