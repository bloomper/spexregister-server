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
import nu.fgv.register.server.acl.PermissionService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivityMapper.SPEX_ACTIVITY_MAPPER;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivitySpecification.hasActivity;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivitySpecification.hasId;
import static nu.fgv.register.server.spexare.activity.spex.SpexActivitySpecification.hasSpex;

/**
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

    @RequiresAdminOrEditorOrUser
    public Page<SpexActivityDto> findByActivity(final Long spexareId, final Long activityId, final Pageable pageable) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> activityRepository.findById(activityId))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(activity -> repository
                            .findAll(hasActivity(activity), pageable)
                            .map(SPEX_ACTIVITY_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class), spexareId, activityId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public SpexActivityDto findById(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(spexActivity -> spexActivity.getActivity().getId().equals(activityId))
                    .filter(spexActivity -> spexActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(SPEX_ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(SpexActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, SpexActivity.class), spexareId, activityId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public SpexActivityDto create(final Long spexareId, final Long activityId, final Long spexId) {
        if (doSpexareAndActivityAndSpexExist(spexareId, activityId, spexId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findById(activityId))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .flatMap(activity -> spexRepository
                            .findById0(spexId)
                            .filter(spex -> !repository.exists(hasActivity(activity).and(hasSpex(spex))))
                            .map(spex -> {
                                final SpexActivity spexActivity = new SpexActivity();
                                spexActivity.setActivity(activity);
                                spexActivity.setSpex(spex);
                                return repository.save(spexActivity);
                            })
                    )
                    .map(SPEX_ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(List.of(Spexare.class, Activity.class, Spex.class), SpexActivity_.SPEX, spexId, spexareId, activityId));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, Spex.class), spexareId, activityId, spexId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void update(final Long spexareId, final Long activityId, final Long spexId, final Long id) {
        if (doSpexareAndActivityAndSpexExist(spexareId, activityId, spexId) && doesSpexActivityExist(id)) {
            spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findById(activityId))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .ifPresentOrElse(
                            activity ->
                                    spexRepository
                                            .findById0(spexId)
                                            .filter(spex -> repository.exists(hasActivity(activity).and(hasId(id))))
                                            .ifPresentOrElse(
                                                    spex ->
                                                            repository
                                                                    .findById(id)
                                                                    .filter(spexActivity -> spexActivity.getActivity().equals(activity))
                                                                    .ifPresentOrElse(
                                                                            spexActivity -> {
                                                                                spexActivity.setSpex(spex);
                                                                                repository.save(spexActivity);
                                                                            },
                                                                            () -> {
                                                                                throw new ResourceNotFoundException(SpexActivity.class, id);
                                                                            }
                                                                    ),
                                                    () -> {
                                                        throw new ResourceNotFoundException(Spex.class, spexId);
                                                    }
                                            ),
                            () -> {
                                throw new ResourceNotFoundException(Activity.class, activityId);
                            }
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, SpexActivity.class, Spex.class), spexareId, activityId, id, spexId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesSpexActivityExist(id)) {
            spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .flatMap(spexare -> activityRepository.findById(activityId))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .filter(activity -> repository.exists(hasActivity(activity).and(hasId(id))))
                    .ifPresentOrElse(
                            activity -> repository
                                    .findById(id)
                                    .filter(spexActivity -> spexActivity.getActivity().equals(activity))
                                    .ifPresentOrElse(
                                            spexActivity -> repository.deleteById(spexActivity.getId()),
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

    @RequiresAdminOrEditorOrUser
    public SpexDto findSpexBySpexActivity(final Long spexareId, final Long activityId, final Long id) {
        if (doSpexareAndActivityExist(spexareId, activityId) && doesSpexActivityExist(id)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(spexActivity -> spexActivity.getActivity().getId().equals(activityId))
                    .filter(spexActivity -> spexActivity.getActivity().getSpexare().getId().equals(spexareId))
                    .map(SpexActivity::getSpex)
                    .map(SPEX_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(SpexActivity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class, SpexActivity.class), spexareId, activityId, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doesSpexActivityExist(final Long id) {
        return repository.findById(id).isPresent();
    }

    private boolean doesActivityExist(final Long id) {
        return activityRepository.findById(id).isPresent();
    }

    private boolean doSpexareAndActivityExist(final Long spexareId, final Long activityId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId);
    }

    private boolean doSpexareAndActivityAndSpexExist(final Long spexareId, final Long activityId, final Long spexId) {
        return doesSpexareExist(spexareId) && doesActivityExist(activityId) && spexRepository.findById0(spexId).isPresent();
    }
}
