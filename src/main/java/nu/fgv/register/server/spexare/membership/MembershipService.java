package nu.fgv.register.server.spexare.membership;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeRepository;
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
                    .map(value -> repository
                            .findBySpexare(value, pageable)
                            .map(MEMBERSHIP_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Page<MembershipDto> findBySpexareAndType(final Long spexareId, final String typeId, final Pageable pageable) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(value -> typeRepository
                            .findById(typeId)
                            .map(type -> repository
                                    .findBySpexareAndType(value, type, pageable)
                                    .map(MEMBERSHIP_MAPPER::toDto)
                            )
                            .orElse(Page.empty())
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or type %s do not exist", spexareId, typeId));
        }
    }

    public Optional<MembershipDto> findById(final Long id) {
        return repository.findById(id).map(MEMBERSHIP_MAPPER::toDto);
    }

    public Optional<MembershipDto> addMembership(final Long spexareId, final String typeId, final String year) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .map(type -> spexareRepository
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
                    )
                    .orElse(null);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or type %s do not exist", spexareId, typeId));
        }
    }

    public boolean removeMembership(final Long spexareId, final String typeId, final String year) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .map(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.existsBySpexareAndTypeAndYear(spexare, type, year))
                            .flatMap(spexare -> repository.findBySpexareAndTypeAndYear(spexare, type, year))
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
        return doesSpexareExist(spexareId) && typeRepository.existsById(typeId);
    }

}
