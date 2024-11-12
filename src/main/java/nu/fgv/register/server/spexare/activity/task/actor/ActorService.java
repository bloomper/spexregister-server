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
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public Page<ActorDto> findByTaskActivity(final Long spexareId, final Long activityId, final Long taskActivityId, final String filter, final Pageable pageable) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, taskActivityId)) {
            return taskActivityRepository
                    .findById0(taskActivityId)
                    .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                    .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(activity -> hasText(filter) ?
                            repository
                                    .findAll(SpecificationsBuilder.<Actor>builder().build(FilterParser.parse(filter), ActorSpecification::new).and(hasTaskActivity(activity)), pageable)
                                    .map(ACTOR_MAPPER::toDto) :
                            repository
                                    .findAll(hasTaskActivity(activity), pageable)
                                    .map(ACTOR_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class), spexareId, activityId, taskActivityId);
        }
    }

    public ActorDto findById(final Long spexareId, final Long activityId, final Long taskActivityId, final Long id) {
        if (doSpexareAndActivityAndTaskActivityExist(spexareId, activityId, taskActivityId)) {
            return repository
                    .findById0(id)
                    .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                    .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                    .map(ACTOR_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Actor.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Actor.class), spexareId, activityId, taskActivityId, id);
        }
    }

    public ActorDto create(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final ActorCreateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId)) {
            return typeRepository
                    .findById(vocalId)
                    .flatMap(vocal -> taskActivityRepository
                            .findById0(taskActivityId)
                            .filter(taskActivity -> taskActivity.getActivity().getId().equals(activityId))
                            .filter(taskActivity -> taskActivity.getActivity().getSpexare().getId().equals(spexareId))
                            .filter(taskActivity -> !repository.exists(hasTaskActivity(taskActivity).and(hasVocal(vocal))))
                            .map(taskActivity -> {
                                final Actor actor = ACTOR_MAPPER.toModel(dto);
                                actor.setTaskActivity(taskActivity);
                                actor.setVocal(vocal);
                                return repository.save(actor);
                            })
                    )
                    .map(ACTOR_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(List.of(Spexare.class, Activity.class, TaskActivity.class, Type.class), Actor_.VOCAL, vocalId, spexareId, activityId, taskActivityId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, TaskActivity.class, Type.class), spexareId, activityId, taskActivityId, vocalId);
        }
    }

    public ActorDto update(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id, final ActorUpdateDto dto) {
        return partialUpdate(spexareId, activityId, taskActivityId, vocalId, id, dto);
    }

    public ActorDto partialUpdate(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id, final ActorUpdateDto dto) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            return typeRepository
                    .findById(vocalId)
                    .flatMap(vocal -> taskActivityRepository
                            .findById0(taskActivityId)
                            .filter(taskActivity -> repository.exists(hasTaskActivity(taskActivity).and(hasVocal(vocal)).and(hasId(id))))
                            .flatMap(taskActivity -> repository.findById0(id))
                            .filter(actor -> actor.getTaskActivity().getId().equals(taskActivityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getId().equals(activityId))
                            .filter(actor -> actor.getTaskActivity().getActivity().getSpexare().getId().equals(spexareId))
                            .map(actor -> {
                                ACTOR_MAPPER.toPartialModel(dto, actor);
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

    public void deleteById(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId, final Long id) {
        if (doSpexareAndActivityAndTaskActivityAndTypeExist(spexareId, activityId, taskActivityId, vocalId) && doesActorExist(id)) {
            typeRepository
                    .findById(vocalId)
                    .ifPresentOrElse(
                            vocal -> taskActivityRepository
                                    .findById0(taskActivityId)
                                    .filter(taskActivity -> repository.exists(hasTaskActivity(taskActivity).and(hasVocal(vocal)).and(hasId(id))))
                                    .flatMap(taskActivity -> repository.findById0(id))
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

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doesActorExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

    private boolean doesTaskActivityExist(final Long id) {
        return taskActivityRepository.findById0(id).isPresent();
    }

    private boolean doesActivityExist(final Long id) {
        return activityRepository.findById0(id).isPresent();
    }

    private boolean doSpexareAndActivityAndTaskActivityExist(final Long spexareId, final Long activityId, final Long taskActivityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId);
    }

    private boolean doSpexareAndActivityAndTaskActivityAndTypeExist(final Long spexareId, final Long activityId, final Long taskActivityId, final String vocalId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && doesTaskActivityExist(taskActivityId) && typeService.existsByIdAndType(vocalId, TypeType.VOCAL);
    }

}
