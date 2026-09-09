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

package nu.fgv.register.server.spexare.activity;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static nu.fgv.register.server.spexare.activity.ActivityMapper.ACTIVITY_MAPPER;
import static nu.fgv.register.server.spexare.activity.ActivitySpecification.hasId;
import static nu.fgv.register.server.spexare.activity.ActivitySpecification.hasSpexare;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ActivityService {

    private final ActivityRepository repository;
    private final SpexareRepository spexareRepository;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<ActivityDto> findBySpexare(final Long id) {
        return findBySpexare(id, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), Pageable.unpaged(Sort.by(Activity_.ID)))
                                .stream()
                                .map(ACTIVITY_MAPPER::toDto)
                                .toList(),
                Collections::emptyList
        );
    }

    @RequiresAdminOrEditorOrUser
    public CountedWindow<ActivityDto> findBySpexare(final Long id, final ScrollRequest scroll, final Sort sort) {
        return findBySpexare(id, spexare ->
                        repository
                                .findBy(hasSpexare(spexare), query -> {
                                    final long total = query.count();

                                    return CountedWindow.of(query
                                            .limit(scroll.limit())
                                            .sortBy(sort)
                                            .scroll(scroll.positionFor(total))
                                            .map(ACTIVITY_MAPPER::toDto), total);
                                }),
                GraphqlUtil::emptyWindow
        );
    }

    @RequiresAdminOrEditorOrUser
    public Page<ActivityDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        return findBySpexare(spexareId, spexare ->
                        repository
                                .findAll(hasSpexare(spexare), pageable)
                                .map(ACTIVITY_MAPPER::toDto),
                Page::empty
        );
    }

    @RequiresAdminOrEditorOrUser
    public ActivityDto findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkReadPermission)
                    .flatMap(spexare -> repository.findById(id))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Activity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class), spexareId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById(id)
                .isPresent();
    }

    @RequiresAdminOrEditorOrUser
    public ActivityDto create(final Long spexareId) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .map(spexare -> {
                        final Activity activity = new Activity();
                        activity.setSpexare(spexare);
                        return repository.save(activity);
                    })
                    .map(ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, spexareId));

        } else {
            throw new ResourceNotFoundException(Spexare.class, spexareId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void deleteById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId) && doesActivityExist(id)) {
            spexareRepository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .filter(spexare -> repository.exists(hasSpexare(spexare).and(hasId(id))))
                    .flatMap(spexare -> repository.findById(id))
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .ifPresentOrElse(
                            activity -> repository.deleteById(activity.getId()),
                            () -> {
                                throw new ResourceNotFoundException(Activity.class, id);
                            });
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class), spexareId, id);
        }
    }

    private <T> T findBySpexare(final Long id, final Function<Spexare, T> queryFunction, final Supplier<T> emptyResult) {
        if (doesSpexareExist(id)) {
            return spexareRepository
                    .findById0(id)
                    .map(permissionService::checkReadPermission)
                    .map(queryFunction)
                    .orElseGet(emptyResult);
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doesActivityExist(final Long id) {
        return repository.findById(id).isPresent();
    }

}
