package nu.fgv.register.server.spexare.activity;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spexare.activity.ActivityMapper.ACTIVITY_MAPPER;
import static nu.fgv.register.server.spexare.activity.ActivitySpecification.hasId;
import static nu.fgv.register.server.spexare.activity.ActivitySpecification.hasSpexare;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ActivityService {

    private final ActivityRepository repository;

    private final SpexareRepository spexareRepository;

    public Page<ActivityDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> repository
                            .findAll(hasSpexare(spexare), pageable)
                            .map(ACTIVITY_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<ActivityDto> findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(id)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(ACTIVITY_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<ActivityDto> create(final Long spexareId) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> {
                        final Activity activity = new Activity();
                        activity.setSpexare(spexare);
                        return repository.save(activity);
                    })
                    .map(ACTIVITY_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public boolean deleteById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId) && doesActivityExist(id)) {
            return spexareRepository
                    .findById(spexareId)
                    .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasId(id))))
                    .flatMap(spexare -> repository.findById(id))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(activity -> {
                        repository.deleteById(activity.getId());
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesActivityExist(final Long id) {
        return repository.existsById(id);
    }

}
