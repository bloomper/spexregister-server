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

package nu.fgv.register.server.spexare.activity.spex;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUserOrRevival;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivityMapper.SPEX_ACTIVITY_MAPPER;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivitySpecification.hasActivity;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivitySpecification.hasId;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivitySpecification.hasSpex;

/**
 * The spex of an activity. Reading needs read access to the spexare and changing write access.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexActivityService {

    private final SpexActivityRepository repository;
    private final ActivityRepository activityRepository;
    private final SpexRepository spexRepository;
    private final SpexareRepository spexareRepository;
    private final PermissionService permissionService;

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @return the spex activity
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public SpexActivityDto findByActivity(final UUID spexareId, final UUID activityId) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> activityRepository.findByUuid(activityId))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .flatMap(activity -> repository
                            .findOne(hasActivity(activity))
                            .map(SPEX_ACTIVITY_MAPPER::toDto))
                    .orElseThrow(() -> new ResourceNotFoundException(Activity.class, activityId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class), spexareId, activityId);
        }
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the spex activity id
     * @return the spex activity
     */
    @RequiresAdminOrEditorOrUser
    public SpexActivityDto findById(final UUID spexareId, final UUID activityId, final UUID id) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findByUuid(id))
                    .filter(spexActivity -> spexActivity.getActivity().getUuid().equals(activityId))
                    .filter(spexActivity -> spexActivity.getActivity().getSpexare().getUuid().equals(spexareId))
                    .map(SPEX_ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(SpexActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, SpexActivity.class), spexareId, activityId, id);
        }
    }

    /**
     * @param id the spex activity id
     * @return whether the spex activity exists
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
     * @param spexId     the spex id
     * @return the created spex activity
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public SpexActivityDto create(final UUID spexareId, final UUID activityId, final UUID spexId) {
        if (doSpexareAndActivityAndSpexExist(spexareId, activityId, spexId)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findByUuid(activityId))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .flatMap(activity -> spexRepository
                            .findByUuid0(spexId)
                            .filter(spex -> !repository.exists(hasActivity(activity).and(hasSpex(spex))))
                            .map(spex -> {
                                final SpexActivity spexActivity = new SpexActivity();
                                spexActivity.setActivity(activity);
                                spexActivity.setSpex(spex);
                                activity.setSpexActivity(spexActivity);
                                return repository.save(spexActivity);
                            })
                    )
                    .map(SPEX_ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(List.of(Spexare.class, Activity.class, Spex.class), SpexActivity_.SPEX, spexId, spexareId, activityId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, Spex.class), spexareId, activityId, spexId);
        }
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param spexId     the spex id
     * @param id         the spex activity id
     * @return the updated spex activity
     */
    @RequiresAdminOrEditorOrUser
    public SpexActivityDto update(final UUID spexareId, final UUID activityId, final UUID spexId, final UUID id) {
        if (doSpexareAndActivityAndSpexExist(spexareId, activityId, spexId) && doesSpexActivityExist(id)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findByUuid(activityId))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .map(activity ->
                            spexRepository
                                    .findByUuid0(spexId)
                                    .filter(spex -> repository.exists(hasActivity(activity).and(hasId(id))))
                                    .map(spex ->
                                            repository
                                                    .findByUuid(id)
                                                    .filter(spexActivity -> spexActivity.getActivity().equals(activity))
                                                    .map(spexActivity -> {
                                                        spexActivity.setSpex(spex);

                                                        return SPEX_ACTIVITY_MAPPER.toDto(repository.save(spexActivity));
                                                    })
                                                    .orElseThrow(() -> new ResourceNotFoundException(SpexActivity.class, id))
                                    )
                                    .orElseThrow(() -> new ResourceNotFoundException(Spex.class, spexId))
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(Activity.class, activityId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, SpexActivity.class, Spex.class), spexareId, activityId, id, spexId);
        }
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the spex activity id
     */
    @RequiresAdminOrEditorOrUser
    public void deleteById(final UUID spexareId, final UUID activityId, final UUID id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesSpexActivityExist(id)) {
            spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findByUuid(activityId))
                    .filter(activity -> activity.getSpexare().getUuid().equals(spexareId))
                    .filter(activity -> repository.exists(hasActivity(activity).and(hasId(id))))
                    .ifPresentOrElse(
                            activity -> repository
                                    .findByUuid(id)
                                    .filter(spexActivity -> spexActivity.getActivity().equals(activity))
                                    .ifPresentOrElse(
                                            spexActivity -> {
                                                activity.setSpexActivity(null);
                                                repository.deleteById(spexActivity.getId());
                                            },
                                            () -> {
                                                throw new ResourceNotFoundException(SpexActivity.class, id);
                                            }
                                    ),
                            () -> {
                                throw new ResourceNotFoundException(Activity.class, activityId);
                            }
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, SpexActivity.class), spexareId, activityId, id);
        }
    }

    /**
     * @param spexareId  the spexare id
     * @param activityId the activity id
     * @param id         the spex activity id
     * @return the spex
     */
    @RequiresAdminOrEditorOrUserOrRevival
    public SpexDto findSpexBySpexActivity(final UUID spexareId, final UUID activityId, final UUID id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesSpexActivityExist(id)) {
            return spexareRepository
                    .findByUuid0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findByUuid(id))
                    .filter(spexActivity -> spexActivity.getActivity().getUuid().equals(activityId))
                    .filter(spexActivity -> spexActivity.getActivity().getSpexare().getUuid().equals(spexareId))
                    .map(SpexActivity::getSpex)
                    .map(SPEX_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(SpexActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, SpexActivity.class), spexareId, activityId, id);
        }
    }

    private boolean doesSpexareExist(final UUID id) {
        return spexareRepository.findByUuid0(id).isPresent();
    }

    private boolean doesSpexActivityExist(final UUID id) {
        return repository.findByUuid(id).isPresent();
    }

    private boolean doesActivityExist(final UUID id) {
        return activityRepository.findByUuid(id).isPresent();
    }

    private boolean doSpexareAndActivityExist(final UUID spexareId, final UUID activityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId);
    }

    private boolean doSpexareAndActivityAndSpexExist(final UUID spexareId, final UUID activityId, final UUID spexId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && spexRepository.findByUuid0(spexId).isPresent();
    }
}
