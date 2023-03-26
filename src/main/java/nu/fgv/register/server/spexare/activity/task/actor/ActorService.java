package nu.fgv.register.server.spexare.activity.task.actor;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.task.TaskActivityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spexare.activity.task.actor.ActorMapper.ACTOR_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ActorService {

    private final ActorRepository repository;

    private final TaskActivityRepository taskActivityRepository;

    private final ActivityRepository activityRepository;

    private final SpexareRepository spexareRepository;

    private final TypeRepository typeRepository;

    public Page<ActorDto> findByTaskActivity(final Long spexareId, final Long activityId, final Long taskActivityId, final Pageable pageable) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, taskActivityId)) {
            return taskActivityRepository
                    .findById(taskActivityId)
                    .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(activity -> repository
                            .findByTaskActivity(activity, pageable)
                            .map(ACTOR_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s and/or task activity %s do not exist", spexareId, activityId, taskActivityId));
        }
    }

    public Optional<ActorDto> findById(final Long spexareId, final Long activityId, final Long taskActivityId, final Long id) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, taskActivityId)) {
            return repository
                    .findById(id)
                    .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                    .map(ACTOR_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s and/or task activity %s do not exist", spexareId, activityId, taskActivityId));
        }
    }

    public Optional<ActorDto> create(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final ActorCreateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId)) {
            return typeRepository
                    .findById(vocalId)
                    .flatMap(vocal -> taskActivityRepository
                            .findById(taskActivityId)
                            .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                            .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                            .filter(taskActivity -> !repository.existsByTaskActivityAndVocal(taskActivity, vocal))
                            .map(taskActivity -> {
                                final Actor actor = ACTOR_MAPPER.toModel(dto);
                                actor.setTaskActivity(taskActivity);
                                actor.setVocal(vocal);
                                return repository.save(actor);
                            })
                            .map(ACTOR_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s, task activity %s and/or vocal %s do not exist", spexareId, activityId, taskActivityId, vocalId));
        }
    }

    public Optional<ActorDto> update(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id, final ActorUpdateDto dto) {
        return partialUpdate(spexareId, activityId, taskActivityId, vocalId, id, dto);
    }

    public Optional<ActorDto> partialUpdate(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id, final ActorUpdateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            return typeRepository
                    .findById(vocalId)
                    .flatMap(vocal -> taskActivityRepository
                            .findById(spexareId)
                            .filter(taskActivity -> repository.existsByTaskActivityAndVocalAndId(taskActivity, vocal, id))
                            .flatMap(taskActivity -> repository.findById(id))
                            .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                            .map(actor -> {
                                ACTOR_MAPPER.toPartialModel(dto, actor);
                                return actor;
                            })
                            .map(repository::save)
                            .map(ACTOR_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s, task activity %s, vocal %s and/or actor %s do not exist", spexareId, activityId, taskActivityId, vocalId, id));
        }
    }

    public boolean deleteById(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            return typeRepository
                    .findById(vocalId)
                    .map(vocal -> taskActivityRepository
                            .findById(taskActivityId)
                            .filter(taskActivity -> repository.existsByTaskActivityAndVocalAndId(taskActivity, vocal, id))
                            .flatMap(taskActivity -> repository.findById(id))
                            .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                            .map(actor -> {
                                repository.deleteById(actor.getId());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s, task activity %s, vocal %s and/or actor %s do not exist", spexareId, activityId, taskActivityId, vocalId, id));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesActorExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doesTaskActivityExist(final Long id) {
        return taskActivityRepository.existsById(id);
    }

    private boolean doesActivityExist(final Long id) {
        return activityRepository.existsById(id);
    }

    private boolean doSpexareAndActivityAndTaskActivityExist(final Long spexareId, final Long activityId, final Long taskActivityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId);
    }

    private boolean doSpexareAndActivityAndTaskActivityAndTypeExist(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId) && typeRepository.existsByIdAndType(vocalId, TypeType.VOCAL);
    }

}
