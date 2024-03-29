package nu.fgv.register.server.spexare.consent;

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

import static nu.fgv.register.server.spexare.consent.ConsentMapper.CONSENT_MAPPER;
import static nu.fgv.register.server.spexare.consent.ConsentSpecification.hasId;
import static nu.fgv.register.server.spexare.consent.ConsentSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.consent.ConsentSpecification.hasType;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ConsentService {

    private final ConsentRepository repository;
    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;
    private final TypeService typeService;

    public Page<ConsentDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> repository
                            .findAll(hasSpexare(spexare), pageable)
                            .map(CONSENT_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<ConsentDto> findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(id)
                    .filter(consent -> consent.getSpexare().getId().equals(spexareId))
                    .map(CONSENT_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<ConsentDto> create(final Long spexareId, final String typeId, final Boolean value) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> !repository.exists(hasSpexare(spexare).and(hasType(type))))
                            .map(spexare -> {
                                final Consent consent = new Consent();
                                consent.setSpexare(spexare);
                                consent.setType(type);
                                consent.setValue(value);
                                return repository.save(consent);
                            })
                            .map(CONSENT_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or type %s do not exist", spexareId, typeId));
        }
    }

    public Optional<ConsentDto> update(final Long spexareId, final String typeId, final Long id, final Boolean value) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesConsentExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(consent -> consent.getSpexare().getId().equals(spexareId))
                            .map(consent -> {
                                consent.setValue(value);
                                return repository.save(consent);
                            })
                            .map(CONSENT_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, type %s and/or consent %s do not exist", spexareId, typeId, id));
        }
    }

    public boolean deleteById(final Long spexareId, final String typeId, final Long id) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesConsentExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .map(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(consent -> consent.getSpexare().getId().equals(spexareId))
                            .map(consent -> {
                                repository.deleteById(consent.getId());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, type %s and/or consent %s do not exist", spexareId, typeId, id));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesConsentExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeService.existsByIdAndType(typeId, TypeType.CONSENT);
    }

}
