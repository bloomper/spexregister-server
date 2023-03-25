package nu.fgv.register.server.spexare.membership;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.SpexareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spexare.membership.MembershipMapper.MEMBERSHIP_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class MembershipService {

    private final MembershipRepository repository;

    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;

    public Page<MembershipDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> repository
                            .findBySpexare(spexare, pageable)
                            .map(MEMBERSHIP_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<MembershipDto> findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(id)
                    .filter(membership -> membership.getSpexare().getId().equals(spexareId))
                    .map(MEMBERSHIP_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<MembershipDto> create(final Long spexareId, final String typeId, final String year) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> !repository.existsBySpexareAndTypeAndYear(spexare, type, year))
                            .map(spexare -> {
                                final Membership membership = new Membership();
                                membership.setSpexare(spexare);
                                membership.setType(type);
                                membership.setYear(year);
                                return repository.save(membership);
                            })
                            .map(MEMBERSHIP_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or type %s do not exist", spexareId, typeId));
        }
    }

    public boolean deleteById(final Long spexareId, final String typeId, final Long id) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .map(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.existsBySpexareAndTypeAndId(spexare, type, id))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(membership -> membership.getSpexare().getId().equals(spexareId))
                            .map(membership -> {
                                repository.deleteById(membership.getId());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or type %s do not exist", spexareId, typeId));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeRepository.existsByIdAndType(typeId, TypeType.MEMBERSHIP);
    }

}
