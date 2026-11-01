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
import nu.fgv.register.server.security.policy.PermissionService;
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

import static nu.fgv.register.server.spexare.activity.task.actor.ActorMapper.ACTOR_MAPPER;
import static nu.fgv.register.server.spexare.activity.task.actor.ActorSpecification.hasTaskActivity;
import static org.springframework.util.StringUtils.hasText;

/**
 * The roles played in a task activity. Reading needs read access to the spexare and changing write access.
 *
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

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the task activity id
     * @return the task activity's actors
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public List<ActorDto> findByTaskActivity(final UUID spexareId, final UUID activityId, final UUID id) {
        return findBySpexareByTaskActivity(spexareId, activityId, id, taskActivity ->
                        repository
                                .findAll(hasTaskActivity(taskActivity), Pageable.unpaged(Sort.by(Actor_.ID)))
                                .stream()
                                .map(ACTOR_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the task activity id
     * @param filter     a filter expression, or blank for all
     * @param scroll     the window to read
     * @param sort       the order
     * @return the window with the total count
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public CountedWindow<ActorDto> findByTaskActivity(final UUID spexareId, final UUID activityId, final UUID id, final String filter, final ScrollRequest scroll, final Sort sort) {
        return findBySpexareByTaskActivity(spexareId, activityId, id, taskActivity ->
                        repository
                                .findBy(hasText(filter) ?
                                                SpecificationsBuilder.<Actor>builder().build(FilterParser.parse(filter), ActorSpecification::new).and(hasTaskActivity(taskActivity)) :
                                                hasTaskActivity(taskActivity),
                                        query -> {
                                            final long total = query.count();

                                            return CountedWindow.of(query
                                                    .limit(scroll.limit())
                                                    .sortBy(sort)
                                                    .scroll(scroll.positionFor(total))
                                                    .map(ACTOR_MAPPER::toDto), total);
                                        }),
                GraphqlUtil::emptyWindow
        );
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the task activity id
     * @param filter     a filter expression, or blank for all
     * @param pageable   the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<ActorDto> findByTaskActivity(final UUID spexareId, final UUID activityId, final UUID id, final String filter, final Pageable pageable) {
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

    /**
     * @param spexareId      the spexare id
     * @param activityId     the activity id
     * @param taskActivityId the task activity id
     * @param id             the actor id
     * @return the actor
     */
    @RequiresAdminOrEditorOrUser
    public ActorDto findById(final UUID spexareId, final UUID activityId, final UUID taskActivityId, final UUID id) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, taskActivityId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findByUuid(id))
                    .filter(actor -> actor.getTaskActivity().getUuid().equals(taskActivityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getUuid().equals(activityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getUuid().equals(spexareId))
                    .map(ACTOR_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Actor.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Actor.class), spexareId, activityId, taskActivityId, id);
        }
    }

    /**
     * @param id the actor id
     * @return whether the actor exists
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final UUID id) {
        return repository
                .findByUuid(id)
                .isPresent();
    }

    /**
     * @param spexareId      the spexare id
     * @param activityId     the activity id
     * @param taskActivityId the task activity id
     * @param vocalId        the id of the vocal type, such as {@code B1}
     * @param dto            the new actor
     * @return the created actor
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public ActorDto create(final UUID spexareId, final UUID activityId, final UUID taskActivityId, final String vocalId, final ActorCreateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> typeRepository.findById(vocalId))
                    .flatMap(vocal -> taskActivityRepository
                            .findByUuid(taskActivityId)
                            .filter(taskActivity -> taskActivity.getActivity().getUuid().equals(activityId))
                            .filter(taskActivity -> taskActivity.getActivity().getSpexare().getUuid().equals(spexareId))
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

    /**
     * @param spexareId      the spexare id
     * @param activityId     the activity id
     * @param taskActivityId the task activity id
     * @param vocalId        the id of the vocal type, such as {@code B1}
     * @param id             the actor id
     * @param dto            the new values
     * @return the updated actor
     */
    @RequiresAdminOrEditorOrUser
    public ActorDto update(final UUID spexareId, final UUID activityId, final UUID taskActivityId, final String vocalId, final UUID id, final ActorUpdateDto dto) {
        return partialUpdate(spexareId, activityId, taskActivityId, vocalId, id, dto);
    }

    /**
     * @param spexareId      the spexare id
     * @param activityId     the activity id
     * @param taskActivityId the task activity id
     * @param vocalId        the id of the vocal type, such as {@code B1}
     * @param id             the actor id
     * @param dto            the new values
     * @return the updated actor
     */
    @RequiresAdminOrEditorOrUser
    public ActorDto partialUpdate(final UUID spexareId, final UUID activityId, final UUID taskActivityId, final String vocalId, final UUID id, final ActorUpdateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> typeRepository.findById(vocalId))
                    .flatMap(vocal -> taskActivityRepository
                            .findByUuid(taskActivityId)
                            .flatMap(taskActivity -> repository.findByUuid(id))
                            .filter(actor -> actor.getTaskActivity().getUuid().equals(taskActivityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getUuid().equals(activityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getUuid().equals(spexareId))
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

    /**
     * @param spexareId      the spexare id
     * @param activityId     the activity id
     * @param taskActivityId the task activity id
     * @param vocalId        the id of the vocal type, such as {@code B1}
     * @param id             the actor id
     */
    @RequiresAdminOrEditorOrUser
    public void deleteById(final UUID spexareId, final UUID activityId, final UUID taskActivityId, final String vocalId, final UUID id) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> typeRepository.findById(vocalId))
                    .ifPresentOrElse(
                            vocal -> taskActivityRepository
                                    .findByUuid(taskActivityId)
                                    .flatMap(taskActivity -> repository.findByUuid(id))
                                    .filter(actor -> actor.getTaskActivity().getUuid().equals(taskActivityId))
                                    .filter(actor -> actor.getTaskActivity().getActivity().getUuid().equals(activityId))
                                    .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getUuid().equals(spexareId))
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

    private <T> T findBySpexareByTaskActivity(final UUID spexareId, final UUID activityId, final UUID id, final Function<TaskActivity, T> queryFunction, final Supplier<T> emptyResult) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, id)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> taskActivityRepository.findByUuid(id))
                    .filter(taskActivity -> taskActivity.getActivity().getUuid().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getUuid().equals(spexareId))
                    .map(queryFunction)
                    .orElseGet(emptyResult);
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, id);
        }
    }

    private boolean doesSpexareExist(final UUID id) {
        return spexareRepository.findByUuid0(id).isPresent();
    }

    private boolean doesActorExist(final UUID id) {
        return repository.findByUuid(id).isPresent();
    }

    private boolean doesTaskActivityExist(final UUID id) {
        return taskActivityRepository.findByUuid(id).isPresent();
    }

    private boolean doesActivityExist(final UUID id) {
        return activityRepository.findByUuid(id).isPresent();
    }

    private boolean doSpexareAndActivityAndTaskActivityExist(final UUID spexareId, final UUID activityId, final UUID taskActivityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId);
    }

    private boolean doSpexareAndActivityAndTaskActivityAndTypeExist(final UUID spexareId, final UUID activityId, final UUID taskActivityId, final String vocalId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId) && typeService.existsByIdAndType(vocalId, TypeType.VOCAL);
    }

}
