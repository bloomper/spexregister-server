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
                            .findBySpexare(spexare, pageable)
                            .map(ACTIVITY_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<ActivityDto> findById(final Long id) {
        return repository.findById(id).map(ACTIVITY_MAPPER::toDto);
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
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .filter(spexare -> repository.existsBySpexareAndId(spexare, id))
                    .flatMap(spexare -> repository.findById(id))
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

}
