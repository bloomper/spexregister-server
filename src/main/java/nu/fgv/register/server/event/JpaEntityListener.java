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

package nu.fgv.register.server.event;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import nu.fgv.register.server.news.News;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.user.User;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class JpaEntityListener {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public JpaEntityListener(final ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostPersist
    private void atCreate(final Object sourceObject) {
        if (isSpexareRelatedChange(sourceObject)) {
            constructEvent(sourceObject, Event.EventType.UPDATE).ifPresent(applicationEventPublisher::publishEvent);
        } else {
            constructEvent(sourceObject, Event.EventType.CREATE).ifPresent(applicationEventPublisher::publishEvent);
        }
    }

    @PreUpdate
    private void atUpdate(final Object sourceObject) {
        constructEvent(sourceObject, Event.EventType.UPDATE).ifPresent(applicationEventPublisher::publishEvent);
    }

    @PreRemove
    private void atRemove(final Object sourceObject) {
        if (isSpexareRelatedChange(sourceObject)) {
            constructEvent(sourceObject, Event.EventType.UPDATE).ifPresent(applicationEventPublisher::publishEvent);
        } else {
            constructEvent(sourceObject, Event.EventType.REMOVE).ifPresent(applicationEventPublisher::publishEvent);
        }
    }

    private Optional<SpringEvent> constructEvent(final Object sourceObject, final Event.EventType event) {
        final Event.SourceType source;
        final Long sourceId;

        if (sourceObject instanceof final News news) {
            source = Event.SourceType.NEWS;
            sourceId = news.getId();
        } else if (sourceObject instanceof final Spex spex) {
            source = Event.SourceType.SPEX;
            sourceId = spex.getId();
        } else if (sourceObject instanceof final SpexCategory category) {
            source = Event.SourceType.SPEX_CATEGORY;
            sourceId = category.getId();
        } else if (sourceObject instanceof final Spexare spexare) {
            source = Event.SourceType.SPEXARE;
            sourceId = spexare.getId();
        } else if (sourceObject instanceof final Tag tag) {
            source = Event.SourceType.TAG;
            sourceId = tag.getId();
        } else if (sourceObject instanceof final Task task) {
            source = Event.SourceType.TASK;
            sourceId = task.getId();
        } else if (sourceObject instanceof final TaskCategory category) {
            source = Event.SourceType.TASK_CATEGORY;
            sourceId = category.getId();
        } else if (sourceObject instanceof final User user) {
            source = Event.SourceType.USER;
            sourceId = user.getId();
        } else if (isSpexareRelatedChange(sourceObject)) {
            source = Event.SourceType.SPEXARE;
            sourceId = extractSpexareId(sourceObject);
        } else {
            source = null;
            sourceId = null;
        }

        if (source != null && sourceId != null) {
            return Optional.of(new SpringEvent(sourceObject, event, source, sourceId));
        }

        return Optional.empty();
    }

    private boolean isSpexareRelatedChange(final Object sourceObject) {
        return sourceObject instanceof Activity ||
                sourceObject instanceof SpexActivity ||
                sourceObject instanceof TaskActivity ||
                sourceObject instanceof Actor ||
                sourceObject instanceof Address ||
                sourceObject instanceof Consent ||
                sourceObject instanceof Membership ||
                sourceObject instanceof Toggle;
    }

    private @Nullable Long extractSpexareId(final Object sourceObject) {
        return switch (sourceObject) {
            case final Activity activity -> activity.getSpexare() != null ? activity.getSpexare().getId() : null;
            case final SpexActivity spexActivity ->
                    spexActivity.getActivity() != null && spexActivity.getActivity().getSpexare() != null ? spexActivity.getActivity().getSpexare().getId() : null;
            case final TaskActivity taskActivity ->
                    taskActivity.getActivity() != null && taskActivity.getActivity().getSpexare() != null ? taskActivity.getActivity().getSpexare().getId() : null;
            case final Actor actor ->
                    actor.getTaskActivity() != null && actor.getTaskActivity().getActivity() != null && actor.getTaskActivity().getActivity().getSpexare() != null ? actor.getTaskActivity().getActivity().getSpexare().getId() : null;
            case final Address address -> address.getSpexare() != null ? address.getSpexare().getId() : null;
            case final Consent consent -> consent.getSpexare() != null ? consent.getSpexare().getId() : null;
            case final Membership membership ->
                    membership.getSpexare() != null ? membership.getSpexare().getId() : null;
            case final Toggle toggle -> toggle.getSpexare() != null ? toggle.getSpexare().getId() : null;
            default -> null;
        };
    }

}
