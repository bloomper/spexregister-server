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
import nu.fgv.register.server.acl.PermissionService;
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
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static nu.fgv.register.server.spexare.activity.task.TaskActivityMapper.TASK_ACTIVITY_MAPPER;
import static nu.fgv.register.server.spexare.activity.task.TaskActivitySpecification.hasActivity;
import static nu.fgv.register.server.spexare.activity.task.TaskActivitySpecification.hasId;
import static nu.fgv.register.server.spexare.activity.task.TaskActivitySpecification.hasTask;
import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;

/**
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

    @RequiresAdminOrEditorOrUser
    public List<TaskActivityDto> findByActivity(final Long spexareId, final Long id) {
        return findBySpexareByActivity(spexareId, id, activity ->
                        repository
                                .findAll(hasActivity(activity), Pageable.unpaged(Sort.by(TaskActivity_.ID)))
                                .stream()
                                .map(TASK_ACTIVITY_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    @RequiresAdminOrEditorOrUser
    public Window<TaskActivityDto> findByActivity(final Long spexareId, final Long id, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return findBySpexareByActivity(spexareId, id, activity ->
                        repository
                                .findBy(hasActivity(activity), query -> query
                                        .limit(limit)
                                        .sortBy(sort)
                                        .scroll(scrollPosition))
                                .map(TASK_ACTIVITY_MAPPER::toDto),
                GraphqlUtil::emptyWindow
        );
    }

    @RequiresAdminOrEditorOrUser
    public Page<TaskActivityDto> findByActivity(final Long spexareId, final Long id, final Pageable pageable) {
        return findBySpexareByActivity(spexareId, id, activity ->
                        repository
                                .findAll(hasActivity(activity), pageable)
                                .map(TASK_ACTIVITY_MAPPER::toDto),
                Page::empty
        );
    }

    @RequiresAdminOrEditorOrUser
    public TaskActivityDto findById(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(TASK_ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(TaskActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public TaskActivityDto create(final Long spexareId, final Long activityId, final Long taskId) {
        if (doSpexareAndActivityAndTaskExist(spexareId, activityId, taskId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findById(activityId))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .flatMap(activity -> taskRepository
                            .findById0(taskId)
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

    @RequiresAdminOrEditorOrUser
    public TaskActivityDto update(final Long spexareId, final Long activityId, final Long taskId, final Long id) {
        if (doSpexareAndActivityAndTaskExist(spexareId, activityId, taskId) && doesTaskActivityExist(id)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findById(activityId))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(activity -> taskRepository
                            .findById0(taskId)
                            .filter(task -> repository.exists(hasActivity(activity).and(hasId(id))))
                            .map(task -> repository
                                    .findById(id)
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

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesTaskActivityExist(id)) {
            spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findById(activityId))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .filter(activity -> repository.exists(hasActivity(activity).and(hasId(id))))
                    .ifPresentOrElse(
                            activity -> repository
                                    .findById(id)
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

    @RequiresAdminOrEditorOrUser
    public TaskDto findTaskByTaskActivity(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesTaskActivityExist(id)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(TaskActivity::getTask)
                    .map(TASK_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(TaskActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, id);
        }
    }

    private <T> T findBySpexareByActivity(final Long spexareId, final Long id, final Function<Activity, T> queryFunction, final Supplier<T> emptyResult) {
        if (doSpexareAndActivityExist(spexareId, id)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> activityRepository.findById(id))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(queryFunction)
                    .orElseGet(emptyResult);
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class), spexareId, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doesTaskActivityExist(final Long id) {
        return repository.findById(id).isPresent();
    }

    private boolean doesActivityExist(final Long id) {
        return activityRepository.findById(id).isPresent();
    }

    private boolean doSpexareAndActivityExist(final Long spexareId, final Long activityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId);
    }

    private boolean doSpexareAndActivityAndTaskExist(final Long spexareId, final Long activityId, final Long taskId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && taskRepository.findById0(taskId).isPresent();
    }
}
