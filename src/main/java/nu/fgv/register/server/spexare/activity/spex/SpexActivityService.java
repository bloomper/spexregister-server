package nu.fgv.register.server.spexare.activity.spex;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivityMapper.SPEX_ACTIVITY_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexActivityService {

    private final SpexActivityRepository repository;

    private final ActivityRepository activityRepository;

    private final SpexRepository spexRepository;

    private final SpexareRepository spexareRepository;

    public Page<SpexActivityDto> findByActivity(final Long spexareId, final Long activityId, final Pageable pageable) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return activityRepository
                    .findById(activityId)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(activity -> repository
                            .findByActivity(activity, pageable)
                            .map(SPEX_ACTIVITY_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or activity %s do not exist", spexareId, activityId));
        }
    }

    public Optional<SpexActivityDto> findById(final Long spexareId, final Long activityId, final Long id) {
        return repository
                .findById(id)
                .filter(spexActivity -> spexActivity.getActivity().getId().equals(activityId) && spexActivity.getActivity().getSpexare().getId().equals(spexareId))
                .map(SPEX_ACTIVITY_MAPPER::toDto);
    }

    public Optional<SpexActivityDto> create(final Long spexareId, final Long activityId, final Long spexId) {
        if (doSpexareAndActivityAndSpexExist(spexareId, activityId, spexId)) {
            return activityRepository
                    .findById(activityId)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .flatMap(activity -> spexRepository
                            .findById(spexId)
                            .filter(spex -> !repository.existsByActivityAndSpex(activity, spex))
                            .map(spex -> {
                                final SpexActivity spexActivity = new SpexActivity();
                                spexActivity.setActivity(activity);
                                spexActivity.setSpex(spex);
                                return repository.save(spexActivity);
                            })
                            .map(SPEX_ACTIVITY_MAPPER::toDto));
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s and/or spex %s do not exist", spexareId, activityId, spexId));
        }
    }

    public boolean update(final Long spexareId, final Long activityId, final Long spexId, final Long id) {
        if (doSpexareAndActivityAndSpexExist(spexareId, activityId, spexId) && doesSpexActivityExist(id)) {
            return activityRepository
                    .findById(activityId)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(activity -> spexRepository
                            .findById(spexId)
                            .filter(spex -> repository.existsByActivityAndId(activity, id))
                            .map(spex -> repository
                                    .findById(id)
                                    .filter(spexActivity -> spexActivity.getActivity().getId().equals(activityId))
                                    .map(spexActivity -> {
                                        spexActivity.setSpex(spex);
                                        repository.save(spexActivity);
                                        return true;
                                    })
                                    .orElse(false)
                            )
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s, spex activity %s and/or spex %s do not exist", spexareId, activityId, id, spexId));
        }
    }

    public boolean deleteById(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return activityRepository
                    .findById(activityId)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .filter(activity -> repository.existsByActivityAndId(activity, id))
                    .map(activity -> repository
                            .findById(id)
                            .filter(spexActivity -> spexActivity.getActivity().getId().equals(activityId))
                            .map(spexActivity -> {
                                repository.deleteById(spexActivity.getId());
                                return true;
                            })
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or activity %s do not exist", spexareId, activityId));
        }
    }

    public Optional<SpexDto> findSpexBySpexActivity(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesSpexActivityExist(id)) {
            return repository
                    .findById(id)
                    .filter(spexActivity -> spexActivity.getActivity().getId().equals(activityId) && spexActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(SpexActivity::getSpex)
                    .map(SPEX_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s and/or spex activity %s do not exist", spexareId, activityId, id));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesSpexActivityExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doesActivityExist(final Long id) {
        return activityRepository.existsById(id);
    }

    private boolean doSpexareAndActivityExist(final Long spexareId, final Long activityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId);
    }

    private boolean doSpexareAndActivityAndSpexExist(final Long spexareId, final Long activityId, final Long spexId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && spexRepository.existsById(spexId);
    }
}
