package nu.fgv.register.server.spexare.activity.task;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.task.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spexare.activity.task.TaskActivityMapper.TASK_ACTIVITY_MAPPER;
import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskActivityService {

    private final TaskActivityRepository repository;

    private final ActivityRepository activityRepository;

    private final TaskRepository taskRepository;

    private final SpexareRepository spexareRepository;

    public Page<TaskActivityDto> findByActivity(final Long spexareId, final Long activityId, final Pageable pageable) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return activityRepository
                    .findById(activityId)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(activity -> repository
                            .findByActivity(activity, pageable)
                            .map(TASK_ACTIVITY_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or activity %s do not exist", spexareId, activityId));
        }
    }

    public Optional<TaskActivityDto> findById(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return repository
                    .findById(id)
                    .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(TASK_ACTIVITY_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or activity %s do not exist", spexareId, activityId));
        }
    }

    public Optional<TaskActivityDto> create(final Long spexareId, final Long activityId, final Long taskId) {
        if (doSpexareAndActivityAndTaskExist(spexareId, activityId, taskId)) {
            return activityRepository
                    .findById(activityId)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .flatMap(activity -> taskRepository
                            .findById(taskId)
                            .filter(task -> !repository.existsByActivityAndTask(activity, task))
                            .map(task -> {
                                final TaskActivity taskActivity = new TaskActivity();
                                taskActivity.setActivity(activity);
                                taskActivity.setTask(task);
                                return repository.save(taskActivity);
                            })
                            .map(TASK_ACTIVITY_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s and/or task %s do not exist", spexareId, activityId, taskId));
        }
    }

    public boolean update(final Long spexareId, final Long activityId, final Long taskId, final Long id) {
        if (doSpexareAndActivityAndTaskExist(spexareId, activityId, taskId) && doesTaskActivityExist(id)) {
            return activityRepository
                    .findById(activityId)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(activity -> taskRepository
                            .findById(taskId)
                            .filter(task -> repository.existsByActivityAndId(activity, id))
                            .map(task -> repository
                                    .findById(id)
                                    .filter(taskActivity -> taskActivity.getActivity().equals(activity))
                                    .map(taskActivity -> {
                                        taskActivity.setTask(task);
                                        repository.save(taskActivity);
                                        return true;
                                    })
                                    .orElse(false)
                            )
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s, task activity %s and/or task %s do not exist", spexareId, activityId, id, taskId));
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
                            .filter(taskActivity -> taskActivity.getActivity().equals(activity))
                            .map(taskActivity -> {
                                repository.deleteById(taskActivity.getId());
                                return true;
                            })
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or activity %s do not exist", spexareId, activityId));
        }
    }

    public Optional<TaskDto> findTaskByTaskActivity(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesTaskActivityExist(id)) {
            return repository
                    .findById(id)
                    .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(TaskActivity::getTask)
                    .map(TASK_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, activity %s and/or task activity %s do not exist", spexareId, activityId, id));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesTaskActivityExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doesActivityExist(final Long id) {
        return activityRepository.existsById(id);
    }

    private boolean doSpexareAndActivityExist(final Long spexareId, final Long activityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId);
    }

    private boolean doSpexareAndActivityAndTaskExist(final Long spexareId, final Long activityId, final Long taskId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && taskRepository.existsById(taskId);
    }
}
