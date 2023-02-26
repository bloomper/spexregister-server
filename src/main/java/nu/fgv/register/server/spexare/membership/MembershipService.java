package nu.fgv.register.server.spexare.membership;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.SpexareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return spexareRepository
                .findById(spexareId)
                .map(value -> repository
                        .findBySpexare(value, pageable)
                        .map(MEMBERSHIP_MAPPER::toDto)
                )
                .orElseGet(Page::empty);
    }

    public Page<MembershipDto> findBySpexareAndType(final Long spexareId, final String typeValue, final Pageable pageable) {
        return spexareRepository
                .findById(spexareId)
                .map(value -> typeRepository
                        .findByTypeAndValue(TypeType.MEMBERSHIP, typeValue)
                        .map(type -> repository
                                .findBySpexareAndType(value, type, pageable)
                                .map(MEMBERSHIP_MAPPER::toDto)
                        )
                        .orElse(Page.empty())
                )
                .orElseGet(Page::empty);
    }

    public Optional<MembershipDto> findById(final Long id) {
        return repository.findById(id).map(MEMBERSHIP_MAPPER::toDto);
    }

    public Optional<MembershipDto> addMembership(final Long spexareId, final String typeValue, final String year) {
        return typeRepository
                .findByTypeAndValue(TypeType.MEMBERSHIP, typeValue)
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
    }

    public boolean removeMembership(final Long spexareId, final String typeValue, final String year) {
        return typeRepository
                .findByTypeAndValue(TypeType.MEMBERSHIP, typeValue)
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

    }
}
