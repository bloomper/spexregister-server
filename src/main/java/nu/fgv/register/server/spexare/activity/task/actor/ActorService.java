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

package nu.fgv.register.server.spexare.activity.task.actor;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivityRepository;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
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

import static nu.fgv.register.server.spexare.activity.task.actor.ActorMapper.ACTOR_MAPPER;
import static nu.fgv.register.server.spexare.activity.task.actor.ActorSpecification.hasId;
import static nu.fgv.register.server.spexare.activity.task.actor.ActorSpecification.hasTaskActivity;
import static nu.fgv.register.server.spexare.activity.task.actor.ActorSpecification.hasVocal;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
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
    private final TypeService typeService;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<ActorDto> findByTaskActivity(final Long spexareId, final Long activityId, final Long id) {
        return findBySpexareByTaskActivity(spexareId, activityId, id, taskActivity ->
                        repository
                                .findAll(hasTaskActivity(taskActivity), Pageable.unpaged(Sort.by(Actor_.ID)))
                                .stream()
                                .map(ACTOR_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    @RequiresAdminOrEditorOrUser
    public Window<ActorDto> findByTaskActivity(final Long spexareId, final Long activityId, final Long id, final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return findBySpexareByTaskActivity(spexareId, activityId, id, taskActivity ->
                        hasText(filter) ?
                                repository
                                        .findBy(SpecificationsBuilder.<Actor>builder().build(FilterParser.parse(filter), ActorSpecification::new).and(hasTaskActivity(taskActivity)), query -> query
                                                .limit(limit)
                                                .sortBy(sort)
                                                .scroll(scrollPosition))
                                        .map(ACTOR_MAPPER::toDto) :
                                repository
                                        .findBy(hasTaskActivity(taskActivity), query -> query
                                                .limit(limit)
                                                .sortBy(sort)
                                                .scroll(scrollPosition))
                                        .map(ACTOR_MAPPER::toDto),
                GraphqlUtil::emptyWindow
        );
    }

    @RequiresAdminOrEditorOrUser
    public Page<ActorDto> findByTaskActivity(final Long spexareId, final Long activityId, final Long id, final String filter, final Pageable pageable) {
        return findBySpexareByTaskActivity(spexareId, activityId, id, taskActivity ->
                        hasText(filter) ?
                                repository
                                        .findAll(SpecificationsBuilder.<Actor>builder().build(FilterParser.parse(filter), ActorSpecification::new).and(hasTaskActivity(taskActivity)), pageable)
                                        .map(ACTOR_MAPPER::toDto) :
                                repository
                                        .findAll(hasTaskActivity(taskActivity), pageable)
                                        .map(ACTOR_MAPPER::toDto),
                Page::empty
        );
    }

    @RequiresAdminOrEditorOrUser
    public ActorDto findById(final Long spexareId, final Long activityId, final Long taskActivityId, final Long id) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, taskActivityId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                    .map(ACTOR_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Actor.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Actor.class), spexareId, activityId, taskActivityId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById(id)
                .isPresent();
    }

    @RequiresAdminOrEditorOrUser
    public ActorDto create(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final ActorCreateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> typeRepository.findById(vocalId))
                    .flatMap(vocal -> taskActivityRepository
                            .findById(taskActivityId)
                            .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                            .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                            .map(taskActivity -> {
                                final Actor actor = ACTOR_MAPPER.toModel(dto);
                                actor.setTaskActivity(taskActivity);
                                actor.setVocal(vocal);
                                return repository.save(actor);
                            })
                    )
                    .map(ACTOR_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(TaskActivity.class, taskActivityId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Type.class), spexareId, activityId, taskActivityId, vocalId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public ActorDto update(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id, final ActorUpdateDto dto) {
        return partialUpdate(spexareId, activityId, taskActivityId, vocalId, id, dto);
    }

    @RequiresAdminOrEditorOrUser
    public ActorDto partialUpdate(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id, final ActorUpdateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> typeRepository.findById(vocalId))
                    .flatMap(vocal -> taskActivityRepository
                            .findById(taskActivityId)
                            .flatMap(taskActivity -> repository.findById(id))
                            .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                            .map(actor -> {
                                ACTOR_MAPPER.toPartialModel(dto, actor);
                                actor.setVocal(vocal);
                                return actor;
                            })
                            .map(repository::save)
                    )
                    .map(ACTOR_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Actor.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Type.class, Actor.class), spexareId, activityId, taskActivityId, vocalId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> typeRepository.findById(vocalId))
                    .ifPresentOrElse(
                            vocal -> taskActivityRepository
                                    .findById(taskActivityId)
                                    .flatMap(taskActivity -> repository.findById(id))
                                    .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                                    .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                                    .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                                    .ifPresentOrElse(
                                            actor -> repository.deleteById(actor.getId()),
                                            () -> {
                                                throw new ResourceNotFoundException(Actor.class, id);
                                            }),
                            () -> {
                                throw new ResourceNotFoundException(Actor.class, id);
                            }
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Type.class, Actor.class), spexareId, activityId, taskActivityId, vocalId, id);
        }
    }

    private <T> T findBySpexareByTaskActivity(final Long spexareId, final Long activityId, final Long id, final Function<TaskActivity, T> queryFunction, final Supplier<T> emptyResult) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, id)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> taskActivityRepository.findById(id))
                    .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(queryFunction)
                    .orElseGet(emptyResult);
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doesActorExist(final Long id) {
        return repository.findById(id).isPresent();
    }

    private boolean doesTaskActivityExist(final Long id) {
        return taskActivityRepository.findById(id).isPresent();
    }

    private boolean doesActivityExist(final Long id) {
        return activityRepository.findById(id).isPresent();
    }

    private boolean doSpexareAndActivityAndTaskActivityExist(final Long spexareId, final Long activityId, final Long taskActivityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId);
    }

    private boolean doSpexareAndActivityAndTaskActivityAndTypeExist(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId) && typeService.existsByIdAndType(vocalId, TypeType.VOCAL);
    }

}
