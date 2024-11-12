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
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public Page<ActivityDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> repository
                            .findAll(hasSpexare(spexare), pageable)
                            .map(ACTIVITY_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(Spexare.class, spexareId);
        }
    }

    public ActivityDto findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(id)
                    .filter(activity -> activity.getSpexare().getId().equals(spexareId))
                    .map(ACTIVITY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Activity.class, id));
        } else {
            throw new ResourcesNotFoundException(List.of(Spexare.class, Activity.class), spexareId, id);
        }
    }

    public ActivityDto create(final Long spexareId) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById0(spexareId)
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

    public void deleteById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId) && doesActivityExist(id)) {
            spexareRepository
                    .findById(spexareId)
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

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.findById0(id).isPresent();
    }

    private boolean doesActivityExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

}
