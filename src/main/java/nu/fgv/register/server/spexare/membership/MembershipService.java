package nu.fgv.register.server.spexare.membership;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spexare.membership.MembershipMapper.MEMBERSHIP_MAPPER;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasId;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasSpexare;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasType;
import static nu.fgv.register.server.spexare.membership.MembershipSpecification.hasYear;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class MembershipService {

    private final MembershipRepository repository;
    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;
    private final TypeService typeService;

    public Page<MembershipDto> findBySpexare(final Long spexareId, final String filter, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> hasText(filter) ?
                            repository
                                    .findAll(SpecificationsBuilder.<Membership>builder().build(FilterParser.parse(filter), MembershipSpecification::new).and(hasSpexare(spexare)), pageable)
                                    .map(MEMBERSHIP_MAPPER::toDto) :
                            repository
                                    .findAll(hasSpexare(spexare), pageable)
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
                            .filter(spexare -> !repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasYear(year))))
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
        if (doSpexareAndTypeExist(spexareId, typeId) && doesMembershipExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .map(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasType(type)).and(hasId(id))))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(membership -> membership.getSpexare().getId().equals(spexareId))
                            .map(membership -> {
                                repository.deleteById(membership.getId());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, type %s and/or membership %s do not exist", spexareId, typeId, id));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesMembershipExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeService.existsByIdAndType(typeId, TypeType.MEMBERSHIP);
    }

}
