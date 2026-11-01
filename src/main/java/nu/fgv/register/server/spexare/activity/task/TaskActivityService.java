/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.spexare.activity.task;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUserOrRevival;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static nu.fgv.register.server.spexare.activity.task.TaskActivityMapper.TASK_ACTIVITY_MAPPER;
import static nu.fgv.register.server.spexare.activity.task.TaskActivitySpecification.hasActivity;
import static nu.fgv.register.server.spexare.activity.task.TaskActivitySpecification.hasId;
import static nu.fgv.register.server.spexare.activity.task.TaskActivitySpecification.hasTask;
import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;

/**
 * The tasks of an activity. Reading needs read access to the spexare and changing write access.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskActivityService {

    private final TaskActivityRepository repository;
    private final ActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final SpexareRepository spexareRepository;
    private final PermissionService permissionService;

    /**
     * @param spexareId the spexare id
     * @param id        the activity id
     * @return the activity's task activities
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public List<TaskActivityDto> findByActivity(final UUID spexareId, final UUID id) {
        return findBySpexareByActivity(spexareId, id, activity ->
                        repository
                                .findAll(hasActivity(activity), Pageable.unpaged(Sort.by(TaskActivity_.ID)))
                                .stream()
                                .map(TASK_ACTIVITY_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    /**
     * @param spexareId the spexare id
     * @param id        the activity id
     * @param scroll    the window to read
     * @param sort      the order
     * @return the window with the total count
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public CountedWindow<TaskActivityDto> findByActivity(final UUID spexareId, final UUID id, final ScrollRequest scroll, final Sort sort) {
        return findBySpexareByActivity(spexareId, id, activity ->
                        repository
                                .findBy(hasActivity(activity), query -> {
                                    final long total = query.count();

                                    return CountedWindow.of(query
                                            .limit(scroll.limit())
                                            .sortBy(sort)
                                            .scroll(scroll.positionFor(total))
                                            .map(TASK_ACTIVITY_MAPPER::toDto), total);
                                }),
                GraphqlUtil::emptyWindow
        );
    }

    /**
     * @param spexareId the spexare id
     * @param id        the activity id
     * @param pageable  the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<TaskActivityDto> findByActivity(final UUID spexareId, final UUID id, final Pageable pageable) {
        return findBySpexareByActivity(spexareId, id, activity ->
                        repository
                                .findAll(hasActivity(activity), pageable)
                                .map(TASK_ACTIVITY_MAPPER::toDto),
                Page::empty
        );
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the task activity id
     * @return the task activity
     */
    @RequiresAdminOrEditorOrUser
    public TaskActivityDto findById(final UUID spexareId, final UUID activityId, final UUID id) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findByUuid(id))
                    .filter(taskActivity -> taskActivity.getActivity().getUuid().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getUuid().equals(spexareId))
                    .map(TASK_ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(TaskActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, id);
        }
    }

    /**
     * @param id the task activity id
     * @return whether the task activity exists
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final UUID id) {
        return repository
                .findByUuid(id)
                .isPresent();
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param taskId     the task id
     * @return the created task activity
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public TaskActivityDto create(final UUID spexareId, final UUID activityId, final UUID taskId) {
        if (doSpexareAndActivityAndTaskExist(spexareId, activityId, taskId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findByUuid(activityId))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .flatMap(activity -> taskRepository
                            .findByUuid0(taskId)
                            .filter(task -> !repository.exists(hasActivity(activity).and(hasTask(task))))
                            .map(task -> {
                                final TaskActivity taskActivity = new TaskActivity();
                                taskActivity.setActivity(activity);
                                taskActivity.setTask(task);
                                return repository.save(taskActivity);
                            })
                    )
                    .map(TASK_ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(List.of(Spexare.class, Activity.class, Task.class), TaskActivity_.TASK, taskId, spexareId, activityId));

        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, Task.class), spexareId, activityId, taskId);
        }
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param taskId     the task id
     * @param id         the task activity id
     * @return the updated task activity
     */
    @RequiresAdminOrEditorOrUser
    public TaskActivityDto update(final UUID spexareId, final UUID activityId, final UUID taskId, final UUID id) {
        if (doSpexareAndActivityAndTaskExist(spexareId, activityId, taskId) && doesTaskActivityExist(id)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findByUuid(activityId))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .map(activity -> taskRepository
                            .findByUuid0(taskId)
                            .filter(task -> repository.exists(hasActivity(activity).and(hasId(id))))
                            .map(task -> repository
                                    .findByUuid(id)
                                    .filter(taskActivity -> taskActivity.getActivity().equals(activity))
                                    .map(taskActivity -> {
                                        taskActivity.setTask(task);

                                        return TASK_ACTIVITY_MAPPER.toDto(repository.save(taskActivity));
                                    })
                                    .orElseThrow(() -> new ResourceNotFoundException(TaskActivity.class, id))
                            )
                            .orElseThrow(() -> new ResourceNotFoundException(Task.class, taskId))
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(Activity.class, activityId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Task.class), spexareId, activityId, id, taskId);
        }
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the task activity id
     */
    @RequiresAdminOrEditorOrUser
    public void deleteById(final UUID spexareId, final UUID activityId, final UUID id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesTaskActivityExist(id)) {
            spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findByUuid(activityId))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .filter(activity -> repository.exists(hasActivity(activity).and(hasId(id))))
                    .ifPresentOrElse(
                            activity -> repository
                                    .findByUuid(id)
                                    .filter(taskActivity -> taskActivity.getActivity().equals(activity))
                                    .ifPresentOrElse(
                                            taskActivity -> repository.deleteById(taskActivity.getId()),
                                            () -> {
                                                throw new ResourceNotFoundException(TaskActivity.class, id);
                                            }),
                            () -> {
                                throw new ResourceNotFoundException(Activity.class, activityId);
                            }
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, id);
        }
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the task activity id
     * @return the task
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public TaskDto findTaskByTaskActivity(final UUID spexareId, final UUID activityId, final UUID id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesTaskActivityExist(id)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findByUuid(id))
                    .filter(taskActivity -> taskActivity.getActivity().getUuid().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getUuid().equals(spexareId))
                    .map(TaskActivity::getTask)
                    .map(TASK_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(TaskActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, id);
        }
    }

    private <T> T findBySpexareByActivity(final UUID spexareId, final UUID id, final Function<Activity, T> queryFunction, final Supplier<T> emptyResult) {
        if (doSpexareAndActivityExist(spexareId, id)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> activityRepository.findByUuid(id))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .map(queryFunction)
                    .orElseGet(emptyResult);
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class), spexareId, id);
        }
    }

    private boolean doesSpexareExist(final UUID id) {
        return spexareRepository.findByUuid0(id).isPresent();
    }

    private boolean doesTaskActivityExist(final UUID id) {
        return repository.findByUuid(id).isPresent();
    }

    private boolean doesActivityExist(final UUID id) {
        return activityRepository.findByUuid(id).isPresent();
    }

    private boolean doSpexareAndActivityExist(final UUID spexareId, final UUID activityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId);
    }

    private boolean doSpexareAndActivityAndTaskExist(final UUID spexareId, final UUID activityId, final UUID taskId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && taskRepository.findByUuid0(taskId).isPresent();
    }
}
